package com.madhavv.enterpriserag.controller;

import com.madhavv.enterpriserag.dto.*;
import com.madhavv.enterpriserag.service.ConfluenceClient;
import com.madhavv.enterpriserag.service.ConfluenceIngestionService;
import com.madhavv.enterpriserag.service.ConfluenceUrlParser;
import com.madhavv.enterpriserag.service.DocumentIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;
    private final ConfluenceClient confluenceClient;
    private final ConfluenceIngestionService confluenceIngestionService;
    private final ConfluenceUrlParser confluenceUrlParser;

    public DocumentController(DocumentIngestionService documentIngestionService, ConfluenceClient confluenceClient, ConfluenceIngestionService confluenceIngestionService, ConfluenceUrlParser confluenceUrlParser) {
        this.documentIngestionService = documentIngestionService;
        this.confluenceClient = confluenceClient;
        this.confluenceIngestionService = confluenceIngestionService;
        this.confluenceUrlParser = confluenceUrlParser;
    }

    @PostMapping("/confluence/{pageId}/ingest")
    public ResponseEntity<Map<String, Object>> ingestConfluencePage(
            @PathVariable String pageId,  @RequestParam("visibility") String visibility) {

        int chunks = confluenceIngestionService.ingest(pageId, visibility);

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
            @RequestParam("file") MultipartFile file,  @RequestParam("visibility") String visibility) throws IOException {

        IngestionResult result = documentIngestionService.ingest(file, visibility);

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
            @RequestParam("files") List<MultipartFile> files, @RequestParam("visibility") String visibility) {

        FolderIngestionResult result = documentIngestionService.ingestFolder(files, visibility);

        HttpStatus status = result.failed() == result.totalFiles() ? HttpStatus.BAD_REQUEST : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(result);
    }

    @GetMapping("/confluence/space/{spaceId}/pages")
    public ResponseEntity<List<ConfluencePageSummary>> getSpacePages(
            @PathVariable String spaceId, @RequestParam("visibility") String visibility) {

        return ResponseEntity.ok(
                confluenceClient.getPagesInSpace(spaceId, visibility)
        );
    }

    @PostMapping("/confluence/page-tree/{pageId}/ingest")
    public ResponseEntity<ConfluenceTreeIngestionResult> ingestPageTree(
            @PathVariable String pageId, @RequestParam("visibility") String visibility) {

        ConfluenceTreeIngestionResult result = confluenceIngestionService.ingestPageTree(pageId, visibility);

        HttpStatus status = result.failed() == result.totalPages()
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(result);
    }

    @PostMapping("/confluence/ingest")
    public ResponseEntity<ConfluenceTreeIngestionResult> ingestConfluence(
            @RequestBody ConfluenceIngestionRequest request, @RequestParam("visibility") String visibility) {

        ConfluenceTreeIngestionResult result =
                confluenceIngestionService.ingestFromUrl(request.url(), visibility);

        HttpStatus status = result.failed() == result.totalPages()
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(result);
    }

    @PostMapping("/confluence/user/ingest")
    public ResponseEntity<ConfluenceTreeIngestionResult> ingestConfluenceForUser(@RequestBody ConfluenceUserIngestionRequest request) {

        ConfluenceUrlParser.ConfluenceUrl parsed = confluenceUrlParser.parse(request.url());

        ConfluenceCredentials credentials =
                new ConfluenceCredentials(
                        request.email(),
                        request.apiToken()
                );

        ConfluenceTreeIngestionResult result;

        switch (parsed.type()) {

            case PAGE -> result =
                    confluenceIngestionService.ingestPageTree(
                            parsed.pageId(),
                            credentials,
                            parsed.baseUrl(),
                            request.visibility()
                    );

            case SPACE -> result =
                    confluenceIngestionService.ingestSpace(
                            parsed.spaceKey(),
                            credentials,
                            parsed.baseUrl(), request.visibility()
                    );

            default -> throw new IllegalArgumentException(
                    "Unsupported Confluence URL"
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}