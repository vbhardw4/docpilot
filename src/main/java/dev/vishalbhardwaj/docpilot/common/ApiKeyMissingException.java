package dev.vishalbhardwaj.docpilot.common;

/** Thrown when a chat/ingest call needs the LLM but no API key is configured. */
public class ApiKeyMissingException extends RuntimeException {

    public ApiKeyMissingException() {
        super("No LLM API key configured. Set the OPENAI_API_KEY environment variable and restart.");
    }
}
