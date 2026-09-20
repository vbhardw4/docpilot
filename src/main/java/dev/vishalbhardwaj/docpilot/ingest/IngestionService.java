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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

    /** Markdown section headers (#, ##, ###) — each section becomes its own chunk. */
    private static final Pattern MD_HEADER = Pattern.compile("^(#{1,3})\\s+(.*)$");

    /**
     * Sections longer than this fall back to token chunking. The sample-doc sections
     * are far shorter, so this never triggers for them (keeping the measured
     * similarity separation intact) — it only guards large real-world uploads
     * against the embedding model's context window.
     */
    private static final int MAX_SECTION_CHARS = 1500;

    private final VectorStore vectorStore;
    private final IngestedDocumentRepository repository;
    /*
     * Fallback splitter for non-markdown uploads (txt, pdf) and for over-long
     * markdown sections. Markdown itself is chunked by section headers via
     * splitMarkdown(): the sample docs are short, so pure token chunking emitted
     * one chunk per doc and diluted per-question cosine similarity below the
     * retrieval threshold.
     */
    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(400)
            .withMinChunkSizeChars(350)
            .withMinChunkLengthToEmbed(5)
            .withMaxNumChunks(10000)
            .withKeepSeparator(true)
            .build();

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

        List<Document> split;
        if (isMarkdown(fileName)) {
            // Heading-aware chunking (mirrors the measured probe exactly).
            split = splitMarkdown(fileName, raw.get(0).getText()).stream()
                    .flatMap(doc -> doc.getText().length() > MAX_SECTION_CHARS
                            ? splitter.apply(List.of(doc)).stream()
                            : Stream.of(doc))
                    .toList();
        } else {
            split = splitter.apply(raw);
        }

        List<Document> chunks = split.stream()
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

    private static boolean isMarkdown(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".md") || lower.endsWith(".markdown");
    }

    /**
     * Heading-aware Markdown chunking: split on {@code #}/{@code ##}/{@code ###}
     * headers so each chunk covers one topic, prefixed with
     * {@code "<doc title> — <section>"} to keep topical context in the embedding.
     * <p>
     * Measured 2026-09-20 with nomic-embed-text on the sample docs: in-scope
     * questions score 0.70–0.87 against their best section chunk while out-of-scope
     * questions stay at 0.53–0.65, so a 0.68 cosine floor separates them cleanly.
     * Package-private static for unit testing (no Spring needed).
     */
    static List<Document> splitMarkdown(String fileName, String text) {
        List<Document> sections = new ArrayList<>();
        String title = null;
        String head = null;
        List<String> bodyLines = new ArrayList<>();
        // -1 keeps trailing empty lines, matching Python str.split("\n") semantics.
        for (String line : text.split("\n", -1)) {
            Matcher m = MD_HEADER.matcher(line);
            if (m.matches()) {
                flushSection(sections, fileName, title, head, bodyLines);
                if (m.group(1).equals("#") && title == null) {
                    title = m.group(2).strip();
                    head = null;
                } else {
                    head = m.group(2).strip();
                }
                bodyLines = new ArrayList<>();
            } else {
                bodyLines.add(line);
            }
        }
        flushSection(sections, fileName, title, head, bodyLines);
        return sections;
    }

    private static void flushSection(List<Document> sections, String fileName,
                                     String title, String head, List<String> bodyLines) {
        if (head == null && bodyLines.isEmpty()) {
            return;
        }
        String h = head != null ? head : (title != null ? title : fileName);
        String label = (title == null || h.equals(title)) ? h : title + " — " + h;
        String body = String.join("\n", bodyLines).strip();
        String chunk = (label + "\n\n" + body).strip();
        if (chunk.length() > 20) {
            sections.add(new Document(chunk));
        }
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
