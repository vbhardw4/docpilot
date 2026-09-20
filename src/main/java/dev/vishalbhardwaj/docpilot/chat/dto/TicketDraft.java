package dev.vishalbhardwaj.docpilot.chat.dto;

/** Pre-filled draft the widget offers when a question escalates to a ticket. */
public record TicketDraft(String subject, String description) {}
