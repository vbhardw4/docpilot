package dev.vishalbhardwaj.docpilot.ingest;

import java.time.Instant;
import java.util.UUID;

public record IngestedDocumentDto(UUID id, String fileName, String contentType, int chunkCount,
                                  long charCount, String status, Instant createdAt) {

    public static IngestedDocumentDto from(IngestedDocument doc) {
        return new IngestedDocumentDto(doc.getId(), doc.getFileName(), doc.getContentType(),
                doc.getChunkCount(), doc.getCharCount(), doc.getStatus().name(), doc.getCreatedAt());
    }
}
