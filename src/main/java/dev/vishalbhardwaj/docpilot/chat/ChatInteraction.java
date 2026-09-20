package dev.vishalbhardwaj.docpilot.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Audit log of every chat question — the raw material for the admin analytics
 * dashboard (answer rate, escalation rate, latency, top questions, feedback).
 */
@Entity
@Table(name = "chat_interactions")
public class ChatInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(nullable = false)
    private boolean answered;

    /** Cosine similarity of the top retrieved chunk; null when nothing was retrieved. */
    private Double topScore;

    @Column(nullable = false)
    private long latencyMs;

    /** Thumbs up/down from the user; null until feedback is given. */
    private Boolean helpful;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ChatInteraction() {
        // JPA
    }

    public ChatInteraction(String sessionId, String question, boolean answered, Double topScore, long latencyMs) {
        this.sessionId = sessionId;
        this.question = question;
        this.answered = answered;
        this.topScore = topScore;
        this.latencyMs = latencyMs;
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getQuestion() {
        return question;
    }

    public boolean isAnswered() {
        return answered;
    }

    public Double getTopScore() {
        return topScore;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public Boolean getHelpful() {
        return helpful;
    }

    public void setHelpful(Boolean helpful) {
        this.helpful = helpful;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
