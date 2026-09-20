package dev.vishalbhardwaj.docpilot.chat;

import dev.vishalbhardwaj.docpilot.chat.dto.Citation;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerComposerTest {

    private static Document doc(String text, String source) {
        return new Document(text, Map.of("source", source));
    }

    @Test
    void formatContext_numbersDocumentsForCitation() {
        String context = AnswerComposer.formatContext(List.of(
                doc("Refunds take 5-10 business days.", "refunds.md"),
                doc("Express costs $16.99.", "shipping.md")));

        assertThat(context).contains("[1] (source: refunds.md)");
        assertThat(context).contains("[2] (source: shipping.md)");
        assertThat(context).contains("Refunds take 5-10 business days.");
    }

    @Test
    void citations_includeSourceAndTrimmedExcerpt() {
        List<Citation> citations = AnswerComposer.citations(List.of(
                doc("a".repeat(500), "refunds.md")));

        assertThat(citations).hasSize(1);
        assertThat(citations.get(0).index()).isEqualTo(1);
        assertThat(citations.get(0).source()).isEqualTo("refunds.md");
        assertThat(citations.get(0).excerpt()).endsWith("...");
        assertThat(citations.get(0).excerpt().length()).isLessThanOrEqualTo(160);
    }

    @Test
    void citations_defaultSourceWhenMetadataMissing() {
        List<Citation> citations = AnswerComposer.citations(List.of(new Document("hello")));

        assertThat(citations.get(0).source()).isEqualTo("unknown");
    }

    @Test
    void formatHistory_emptyWhenNoPriorMessages() {
        assertThat(AnswerComposer.formatHistory(List.of())).contains("no prior messages");
    }

    @Test
    void formatHistory_rendersRolesInOrder() {
        List<ConversationMessage> history = List.of(
                new ConversationMessage("s1", MessageRole.USER, "hi"),
                new ConversationMessage("s1", MessageRole.ASSISTANT, "hello"));

        String formatted = AnswerComposer.formatHistory(history);

        assertThat(formatted).contains("USER: hi");
        assertThat(formatted).contains("ASSISTANT: hello");
        assertThat(formatted.indexOf("USER")).isLessThan(formatted.indexOf("ASSISTANT"));
    }

    @Test
    void systemPrompt_forbidsHallucination() {
        assertThat(AnswerComposer.SYSTEM_PROMPT).contains("ONLY");
        assertThat(AnswerComposer.SYSTEM_PROMPT).contains("[1]");
    }

    @Test
    void notFoundMessage_offersTicket() {
        assertThat(AnswerComposer.notFoundMessage()).contains("support ticket");
    }
}
