package dev.vishalbhardwaj.docpilot.chat;

import dev.vishalbhardwaj.docpilot.chat.dto.ChatMessageDto;
import dev.vishalbhardwaj.docpilot.chat.dto.ChatRequest;
import dev.vishalbhardwaj.docpilot.chat.dto.ChatResponse;
import dev.vishalbhardwaj.docpilot.chat.dto.FeedbackRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    public ChatController(ChatService chatService, ConversationService conversationService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }

    /** Ask a question against the ingested docs. The widget posts here. */
    @PostMapping
    public ChatResponse ask(@Valid @RequestBody ChatRequest request) {
        return chatService.ask(request);
    }

    /** Full message history for a session (admin/debug view). */
    @GetMapping("/{sessionId}/history")
    public List<ChatMessageDto> history(@PathVariable String sessionId) {
        return conversationService.history(sessionId).stream()
                .map(m -> new ChatMessageDto(m.getRole().name(), m.getContent(), m.getCreatedAt()))
                .toList();
    }

    /** Thumbs up/down on an answer, shown in the widget under each reply. */
    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void feedback(@Valid @RequestBody FeedbackRequest request) {
        chatService.recordFeedback(request);
    }
}
