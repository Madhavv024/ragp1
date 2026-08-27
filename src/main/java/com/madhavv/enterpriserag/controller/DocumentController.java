package com.madhavv.enterpriserag.controller;

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

    public DocumentController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @PostMapping
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