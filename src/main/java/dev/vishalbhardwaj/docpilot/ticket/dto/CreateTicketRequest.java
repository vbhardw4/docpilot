package dev.vishalbhardwaj.docpilot.ticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank(message = "subject must not be blank") String subject,
        String description,
        String requesterName,
        @Email(message = "requesterEmail must be a valid email") String requesterEmail) {}
