package dev.vishalbhardwaj.docpilot.common;

import java.time.Instant;

/** Uniform error payload returned by {@link GlobalExceptionHandler}. */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {}
