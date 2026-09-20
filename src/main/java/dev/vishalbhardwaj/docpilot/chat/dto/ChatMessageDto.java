package dev.vishalbhardwaj.docpilot.chat.dto;

import java.time.Instant;

public record ChatMessageDto(String role, String content, Instant createdAt) {}
