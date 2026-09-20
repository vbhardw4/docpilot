package dev.vishalbhardwaj.docpilot.ticket;

import dev.vishalbhardwaj.docpilot.ticket.dto.CreateTicketRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final SupportTicketRepository repository;

    public TicketService(SupportTicketRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SupportTicket create(CreateTicketRequest request) {
        return repository.save(new SupportTicket(
                request.subject().trim(),
                request.description(),
                request.requesterName(),
                request.requesterEmail()));
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> list(TicketStatus status) {
        if (status != null) {
            return repository.findAllByStatusOrderByCreatedAtDesc(status);
        }
        return repository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    @Transactional
    public SupportTicket updateStatus(UUID id, TicketStatus status) {
        SupportTicket ticket = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
        ticket.setStatus(status);
        return ticket;
    }
}
