package dev.vishalbhardwaj.docpilot.chat;

import dev.vishalbhardwaj.docpilot.chat.dto.ChatRequest;
import dev.vishalbhardwaj.docpilot.chat.dto.ChatResponse;
import dev.vishalbhardwaj.docpilot.chat.dto.FeedbackRequest;
import dev.vishalbhardwaj.docpilot.chat.dto.TicketDraft;
import dev.vishalbhardwaj.docpilot.common.LlmUnavailableException;
import dev.vishalbhardwaj.docpilot.config.DocPilotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * The RAG pipeline:
 * <ol>
 *   <li>Embed the question and retrieve top-k chunks from pgvector above the similarity threshold.</li>
 *   <li>If nothing clears the threshold → <b>escalate</b>: no LLM call, no guessing;
 *       return the guardrail reply plus a pre-filled support-ticket draft.</li>
 *   <li>Otherwise build a citation-enforcing prompt (system + numbered context +
 *       conversation history) and call the chat model.</li>
 *   <li>Persist the turn (conversation memory) and log the interaction (analytics).</li>
 * </ol>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ConversationService conversations;
    private final ChatInteractionRepository interactions;
    private final DocPilotProperties properties;
    private final String ollamaBaseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public ChatService(ChatClient.Builder chatClientBuilder,
                       VectorStore vectorStore,
                       ConversationService conversations,
                       ChatInteractionRepository interactions,
                       DocPilotProperties properties,
                       @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.conversations = conversations;
        this.interactions = interactions;
        this.properties = properties;
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    /** Fail fast with a clear 503 when the local LLM backend is not running. */
    private void ensureLlmAvailable() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(ollamaBaseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<Void> res = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            if (res.statusCode() >= 400) {
                throw new LlmUnavailableException();
            }
        } catch (LlmUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmUnavailableException();
        }
    }

    @Transactional
    public ChatResponse ask(ChatRequest request) {
        ensureLlmAvailable();
        long started = System.currentTimeMillis();
        String sessionId = StringUtils.hasText(request.sessionId())
                ? request.sessionId()
                : UUID.randomUUID().toString();
        String question = request.question().trim();

        List<Document> hits = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(properties.retrieval().topK())
                .similarityThreshold(properties.retrieval().similarityThreshold())
                .build());

        List<ConversationMessage> history = conversations.recent(sessionId, properties.chat().historyWindow());

        ChatResponse response;
        Double topScore = hits.isEmpty() ? null : hits.get(0).getScore();
        if (hits.isEmpty()) {
            response = ChatResponse.escalated(sessionId, AnswerComposer.notFoundMessage(),
                    new TicketDraft("Question: " + truncate(question, 80),
                            "Asked in chat (session " + sessionId + "):\n\n" + question));
            log.info("Escalated question, nothing above threshold: {}", question);
        } else {
            String answer = chatClient.prompt()
                    .system(AnswerComposer.SYSTEM_PROMPT)
                    .user(u -> u.text(AnswerComposer.USER_TEMPLATE)
                            .param("history", AnswerComposer.formatHistory(history))
                            .param("context", AnswerComposer.formatContext(hits))
                            .param("question", question))
                    .call()
                    .content();
            response = ChatResponse.answered(sessionId, answer,
                    AnswerComposer.citations(hits), topScore != null ? topScore : 0.0);
        }

        conversations.saveTurn(sessionId, question, response.answer());
        ChatInteraction interaction = interactions.save(new ChatInteraction(
                sessionId, question, !response.escalated(), topScore, System.currentTimeMillis() - started));
        return response.withInteractionId(interaction.getId());
    }

    @Transactional
    public void recordFeedback(FeedbackRequest request) {
        ChatInteraction interaction = interactions.findById(request.interactionId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown interaction: " + request.interactionId()));
        interaction.setHelpful(request.helpful());
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}
