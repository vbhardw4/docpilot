package dev.vishalbhardwaj.docpilot.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        /** Opaque client session id; a new one is minted when blank. */
        String sessionId,
        @NotBlank(message = "question must not be blank") String question) {}
