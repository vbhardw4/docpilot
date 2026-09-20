package dev.vishalbhardwaj.docpilot.ingest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata row for an uploaded source document. The actual text chunks and their
 * embeddings live in the pgvector {@code vector_store} table (managed by Spring AI);
 * each chunk carries {@code docpilot_doc_id} metadata linking back to this row.
 */
@Entity
@Table(name = "ingested_documents")
public class IngestedDocument {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String fileName;

    private String contentType;

    @Column(nullable = false)
    private int chunkCount;

    @Column(nullable = false)
    private long charCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.READY;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected IngestedDocument() {
        // JPA
    }

    public IngestedDocument(String fileName, String contentType, int chunkCount, long charCount) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.chunkCount = chunkCount;
        this.charCount = charCount;
    }

    public UUID getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public long getCharCount() {
        return charCount;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
