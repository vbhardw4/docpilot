package dev.vishalbhardwaj.docpilot.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Seeds the demo knowledge base on first boot from {@code classpath:/sample-docs/*.md}
 * (a fictional parcel-tracking SaaS help center) so the chatbot answers questions
 * immediately after {@code docker compose up}. Runs only when no documents exist.
 */
@Component
public class SampleDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

    private final IngestionService ingestionService;
    private final IngestedDocumentRepository repository;

    public SampleDataLoader(IngestionService ingestionService, IngestedDocumentRepository repository) {
        this.ingestionService = ingestionService;
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (repository.count() > 0) {
            return;
        }
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/sample-docs/*.md");
        for (Resource resource : resources) {
            String fileName = resource.getFilename() != null ? resource.getFilename() : "sample.md";
            try (InputStream in = resource.getInputStream()) {
                ingestionService.ingest(fileName, "text/markdown", in);
            }
        }
        log.info("Seeded {} sample documents", resources.length);
    }
}
