package dev.vishalbhardwaj.docpilot.ingest;

import java.util.UUID;

public record DocumentUploadResponse(UUID id, String fileName, int chunkCount, String status) {}
