package dev.vishalbhardwaj.docpilot.chat.dto;

import java.util.List;

public record ChatResponse(String sessionId, Long interactionId, String answer,
                           List<Citation> citations, Double confidence,
                           boolean escalated, TicketDraft ticketDraft) {

    public static ChatResponse answered(String sessionId, String answer,
                                        List<Citation> citations, double confidence) {
        return new ChatResponse(sessionId, null, answer, citations, confidence, false, null);
    }

    public static ChatResponse escalated(String sessionId, String answer, TicketDraft ticketDraft) {
        return new ChatResponse(sessionId, null, answer, List.of(), null, true, ticketDraft);
    }

    public ChatResponse withInteractionId(Long id) {
        return new ChatResponse(sessionId, id, answer, citations, confidence, escalated, ticketDraft);
    }
}
