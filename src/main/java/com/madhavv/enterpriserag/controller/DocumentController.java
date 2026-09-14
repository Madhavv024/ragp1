package com.madhavv.enterpriserag.controller;

import com.madhavv.enterpriserag.dto.*;
import com.madhavv.enterpriserag.service.ConfluenceClient;
import com.madhavv.enterpriserag.service.ConfluenceIngestionService;
import com.madhavv.enterpriserag.service.DocumentIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;
    private final ConfluenceClient confluenceClient;
    private final ConfluenceIngestionService confluenceIngestionService;

    public DocumentController(DocumentIngestionService documentIngestionService, ConfluenceClient confluenceClient, ConfluenceIngestionService confluenceIngestionService) {
        this.documentIngestionService = documentIngestionService;
        this.confluenceClient = confluenceClient;
        this.confluenceIngestionService = confluenceIngestionService;
    }

    @PostMapping("/confluence/{pageId}/ingest")
    public ResponseEntity<Map<String, Object>> ingestConfluencePage(
            @PathVariable String pageId) {

        int chunks = confluenceIngestionService.ingest(pageId);

        return ResponseEntity.ok(
                Map.of(
                        "pageId", pageId,
                        "chunks", chunks,
                        "status", "INGESTED"
                )
        );
    }

    @GetMapping("/confluence/test/{pageId}")
    public ResponseEntity<ConfluencePage> testConfluence(
            @PathVariable String pageId) {

        return ResponseEntity.ok(
                confluenceClient.getPage(pageId)
        );
    }

    @PostMapping(value = "upload")
    public ResponseEntity<Map<String, String>> uploadDocument(
            @RequestParam("file") MultipartFile file) throws IOException {

        IngestionResult result = documentIngestionService.ingest(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "documentId", result.documentId(),
                        "filename", result.filename(),
                        "chunksCreated", String.valueOf(result.chunksCreated())
                )
        );
    }

    @PostMapping("/upload-folder")
    public ResponseEntity<FolderIngestionResult> uploadFolder(
            @RequestParam("files") List<MultipartFile> files) {

        FolderIngestionResult result = documentIngestionService.ingestFolder(files);

        HttpStatus status = result.failed() == result.totalFiles() ? HttpStatus.BAD_REQUEST : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(result);
    }

    @GetMapping("/confluence/space/{spaceId}/pages")
    public ResponseEntity<List<ConfluencePageSummary>> getSpacePages(
            @PathVariable String spaceId) {

        return ResponseEntity.ok(
                confluenceClient.getPagesInSpace(spaceId)
        );
    }

    @PostMapping("/confluence/page-tree/{pageId}/ingest")
    public ResponseEntity<ConfluenceTreeIngestionResult> ingestPageTree(
            @PathVariable String pageId) {

        ConfluenceTreeIngestionResult result = confluenceIngestionService.ingestPageTree(pageId);

        HttpStatus status = result.failed() == result.totalPages()
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(result);
    }

    @PostMapping("/confluence/ingest")
    public ResponseEntity<ConfluenceTreeIngestionResult> ingestConfluence(
            @RequestBody ConfluenceIngestionRequest request) {

        ConfluenceTreeIngestionResult result =
                confluenceIngestionService.ingestFromUrl(request.url());

        HttpStatus status = result.failed() == result.totalPages()
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(result);
    }
}