package dev.vishalbhardwaj.docpilot.common;

/** Thrown when a chat call needs the LLM but the local Ollama server is not reachable. */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException() {
        super("LLM backend is not reachable. Start it with `ollama serve` (and pull the models) and retry.");
    }
}
