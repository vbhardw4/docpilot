package dev.vishalbhardwaj.docpilot.ingest;

import dev.vishalbhardwaj.docpilot.ingest.dto.DocumentUploadResponse;
import dev.vishalbhardwaj.docpilot.ingest.dto.IngestedDocumentDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /** Upload a .pdf / .md / .txt help-center document. It is chunked, embedded and indexed. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        IngestedDocument doc = ingestionService.ingest(
                file.getOriginalFilename(), file.getContentType(), file.getInputStream());
        return ResponseEntity.accepted().body(new DocumentUploadResponse(
                doc.getId(), doc.getFileName(), doc.getChunkCount(), doc.getStatus().name()));
    }

    /** List ingested source documents (admin view of the knowledge base). */
    @GetMapping
    public List<IngestedDocumentDto> list() {
        return ingestionService.list().stream().map(IngestedDocumentDto::from).toList();
    }

    /** Delete a document and exactly the vector chunks that belong to it. */
    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        ingestionService.delete(id);
    }
}
