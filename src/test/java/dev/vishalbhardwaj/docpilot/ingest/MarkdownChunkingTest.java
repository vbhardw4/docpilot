package dev.vishalbhardwaj.docpilot.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the heading-aware chunking that the retrieval threshold is tuned for:
 * measured 2026-09-20 with nomic-embed-text, in-scope eval questions score
 * 0.70–0.87 against their best section chunk while out-of-scope questions stay
 * at 0.53–0.65. If the sample docs change shape, these tests fail loudly so the
 * threshold gets re-measured instead of silently degrading.
 */
class MarkdownChunkingTest {

    private static String sampleDoc(String name) throws Exception {
        try (InputStream in = MarkdownChunkingTest.class.getResourceAsStream("/sample-docs/" + name)) {
            return new String(Objects.requireNonNull(in, name).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void splitsOnSectionHeaders_withTitleAndSectionPrefix() {
        String md = "# Shipping rates\n\nIntro paragraph.\n\n## Cutoff times\n\nOrder by 3pm.\n\n## Coverage\n\nOntario only.\n";
        List<Document> chunks = IngestionService.splitMarkdown("shipping.md", md);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).getText()).isEqualTo("Shipping rates\n\nIntro paragraph.");
        assertThat(chunks.get(1).getText()).isEqualTo("Shipping rates — Cutoff times\n\nOrder by 3pm.");
        assertThat(chunks.get(2).getText()).isEqualTo("Shipping rates — Coverage\n\nOntario only.");
    }

    @Test
    void sampleDocs_yieldOneChunkPerSection() throws Exception {
        // shipping.md: intro + Service levels + Coverage + Cutoff times + address change
        assertThat(IngestionService.splitMarkdown("shipping.md", sampleDoc("shipping.md"))).hasSize(5);
        // refunds.md: title-only intro + Filing a claim + What is covered + Refund timeline + NOT covered
        assertThat(IngestionService.splitMarkdown("refunds.md", sampleDoc("refunds.md"))).hasSize(5);
        // getting-started.md: intro + 4 sections
        assertThat(IngestionService.splitMarkdown("getting-started.md", sampleDoc("getting-started.md"))).hasSize(5);
    }

    @Test
    void everyChunk_isNonTrivial() throws Exception {
        for (String name : List.of("shipping.md", "refunds.md", "getting-started.md")) {
            for (Document chunk : IngestionService.splitMarkdown(name, sampleDoc(name))) {
                assertThat(chunk.getText()).hasSizeGreaterThan(20);
            }
        }
    }
}
