package com.madhavv.enterpriserag.controller;

import com.madhavv.enterpriserag.dto.ConfluencePage;
import com.madhavv.enterpriserag.service.ConfluenceClient;
import com.madhavv.enterpriserag.service.ConfluenceIngestionService;
import com.madhavv.enterpriserag.service.DocumentIngestionService;
import com.madhavv.enterpriserag.service.IngestionResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
}