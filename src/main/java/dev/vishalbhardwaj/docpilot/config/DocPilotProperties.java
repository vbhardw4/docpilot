package dev.vishalbhardwaj.docpilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning knobs for retrieval and chat, bound from {@code docpilot.*} in application.yml.
 * Keeping these in config (not code) makes the demo tunable live in front of a buyer.
 */
@ConfigurationProperties(prefix = "docpilot")
public record DocPilotProperties(Retrieval retrieval, Chat chat) {

    public record Retrieval(int topK, double similarityThreshold) {}

    public record Chat(int historyWindow) {}
}
