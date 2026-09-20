package dev.vishalbhardwaj.docpilot.common;

/** Thrown when a chat call needs the LLM but no Gemini API key is configured. */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException() {
        super("LLM backend is not configured. Set the GEMINI_API_KEY environment variable "
                + "(free key at https://aistudio.google.com) and retry.");
    }
}
