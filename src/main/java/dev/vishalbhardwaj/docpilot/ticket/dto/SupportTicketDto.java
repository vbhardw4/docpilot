package dev.vishalbhardwaj.docpilot.ticket.dto;

import dev.vishalbhardwaj.docpilot.ticket.SupportTicket;

import java.time.Instant;
import java.util.UUID;

public record SupportTicketDto(UUID id, String subject, String description, String requesterName,
                               String requesterEmail, String status, Instant createdAt) {

    public static SupportTicketDto from(SupportTicket ticket) {
        return new SupportTicketDto(ticket.getId(), ticket.getSubject(), ticket.getDescription(),
                ticket.getRequesterName(), ticket.getRequesterEmail(),
                ticket.getStatus().name(), ticket.getCreatedAt());
    }
}
