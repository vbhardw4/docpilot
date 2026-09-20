package dev.vishalbhardwaj.docpilot.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ingestion pipeline: read → chunk → embed → store in pgvector.
 * <p>
 * Supported inputs: PDF (per-page reading) and UTF-8 text/markdown. Every chunk is
 * tagged with {@code docpilot_doc_id} metadata so {@link #delete(UUID)} can remove
 * exactly the vectors that belong to a document.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    /** Metadata key linking vector chunks back to their source document row. */
    static final String DOC_ID_METADATA_KEY = "docpilot_doc_id";

    private static final long MAX_BYTES = 10 * 1024 * 1024;

    private final VectorStore vectorStore;
    private final IngestedDocumentRepository repository;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public IngestionService(VectorStore vectorStore, IngestedDocumentRepository repository) {
        this.vectorStore = vectorStore;
        this.repository = repository;
    }

    @Transactional
    public IngestedDocument ingest(String fileName, String contentType, InputStream content) throws IOException {
        byte[] bytes = content.readAllBytes();
        validate(fileName, bytes.length);

        UUID docId = UUID.randomUUID();
        List<Document> raw = read(fileName, bytes);
        long charCount = raw.stream().mapToLong(d -> d.getText() == null ? 0 : d.getText().length()).sum();

        List<Document> chunks = splitter.apply(raw).stream()
                .map(chunk -> {
                    Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                    metadata.put(DOC_ID_METADATA_KEY, docId.toString());
                    metadata.put("source", fileName);
                    return new Document(chunk.getId(), chunk.getText(), metadata);
                })
                .toList();

        // Embedding happens inside add(): each chunk is embedded via the configured
        // EmbeddingModel, then inserted into the pgvector table in the same transaction.
        vectorStore.add(chunks);

        IngestedDocument saved = repository.save(new IngestedDocument(fileName, contentType, chunks.size(), charCount));
        log.info("Ingested {} ({} chars -> {} chunks)", fileName, charCount, chunks.size());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        IngestedDocument doc = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        vectorStore.delete(new FilterExpressionBuilder().eq(DOC_ID_METADATA_KEY, id.toString()).build());
        repository.delete(doc);
        log.info("Deleted document {} and its {} vector chunks", id, doc.getChunkCount());
    }

    @Transactional(readOnly = true)
    public List<IngestedDocument> list() {
        return repository.findAll();
    }

    private List<Document> read(String fileName, byte[] bytes) throws IOException {
        if (fileName.toLowerCase().endsWith(".pdf")) {
            try (InputStream in = new java.io.ByteArrayInputStream(bytes)) {
                return new PagePdfDocumentReader(new InputStreamResource(in)).read();
            }
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.isBlank()) {
            throw new IllegalArgumentException("File is empty: " + fileName);
        }
        return List.of(new Document(text));
    }

    private void validate(String fileName, long size) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("A file name is required");
        }
        String lower = fileName.toLowerCase();
        if (!(lower.endsWith(".pdf") || lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt"))) {
            throw new IllegalArgumentException("Unsupported file type (use .pdf, .md or .txt): " + fileName);
        }
        if (size > MAX_BYTES) {
            throw new IllegalArgumentException("File too large (max 10MB): " + fileName);
        }
    }
}
