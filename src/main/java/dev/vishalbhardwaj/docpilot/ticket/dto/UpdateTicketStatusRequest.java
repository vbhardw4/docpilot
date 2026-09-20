package dev.vishalbhardwaj.docpilot.ticket.dto;

import dev.vishalbhardwaj.docpilot.ticket.SupportTicket;
import dev.vishalbhardwaj.docpilot.ticket.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(@NotNull(message = "status is required") TicketStatus status) {}
