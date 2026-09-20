package dev.vishalbhardwaj.docpilot.chat.dto;

import jakarta.validation.constraints.NotNull;

public record FeedbackRequest(@NotNull(message = "interactionId is required") Long interactionId,
                              @NotNull(message = "helpful is required") Boolean helpful) {}
