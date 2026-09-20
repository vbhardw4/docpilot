package dev.vishalbhardwaj.docpilot.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Support ticket created when the chatbot cannot answer from the docs —
 * the "I don't know" guardrail made concrete. In a client deployment this would
 * forward to Zendesk/Intercom/Freshdesk; here it is stored locally.
 */
@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String requesterName;

    private String requesterEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SupportTicket() {
        // JPA
    }

    public SupportTicket(String subject, String description, String requesterName, String requesterEmail) {
        this.subject = subject;
        this.description = description;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
    }

    public UUID getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
