package dev.vishalbhardwaj.docpilot.ingest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IngestedDocumentRepository extends JpaRepository<IngestedDocument, UUID> {
}
