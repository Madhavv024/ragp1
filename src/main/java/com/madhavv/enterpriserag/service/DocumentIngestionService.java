package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.FolderIngestionResult;
import com.madhavv.enterpriserag.dto.IngestionResult;
import com.madhavv.enterpriserag.repository.DocumentRepository;
import com.madhavv.enterpriserag.repository.RagActivityLogRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final OverlapTokenTextSplitter textSplitter;
    private final GuardrailService guardrailService;
    private final RagActivityLogRepository ragActivityLogRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final DocumentRepository documentRepository;

    public DocumentIngestionService(VectorStore vectorStore, RagActivityLogRepository ragActivityLogRepository, AuthenticatedUserService authenticatedUserService, OverlapTokenTextSplitter textSplitter, GuardrailService guardrailService, DocumentRepository documentRepository) {
        this.vectorStore = vectorStore;
        this.ragActivityLogRepository = ragActivityLogRepository;
        this.authenticatedUserService = authenticatedUserService;

        /*this.textSplitter = TokenTextSplitter.builder()
                .withChunkSize(400)
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .build();*/

        this.textSplitter = textSplitter;
        this.guardrailService = guardrailService;
        this.documentRepository = documentRepository;
    }

    public IngestionResult ingest(MultipartFile file, String visibility) throws IOException{

        UUID userId = authenticatedUserService.getCurrentUserId();
        UUID documentId = UUID.randomUUID();

        TikaDocumentReader reader = new TikaDocumentReader(file.getResource());

        List<Document> documents = reader.get();

        documents = documents.stream().map(document -> new Document(guardrailService.sanitize(document.getText()),document.getMetadata()))
                .toList();

        Map<String, Object> metadata = new HashMap<>();

        if (!"PRIVATE".equalsIgnoreCase(visibility)
                && !"PUBLIC".equalsIgnoreCase(visibility)) {
            throw new IllegalArgumentException(
                    "Visibility must be either PRIVATE or PUBLIC"
            );
        }


        metadata.put("documentId", documentId.toString());
        metadata.put("filename", file.getOriginalFilename());
        metadata.put("contentType", file.getContentType());
        metadata.put("sourceType", "DOCUMENTS");
        metadata.put("userId", userId.toString());
        metadata.put("visibility", visibility.toUpperCase());

        documents.forEach(document ->
                document.getMetadata().putAll(metadata)
        );

        List<Document> chunks = textSplitter.apply(documents);

        System.out.println("Documents before splitting: " + documents.size());
        System.out.println("Chunks after splitting: " + chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put("chunkIndex", i);
        }

        vectorStore.add(chunks);

        documentRepository.save(
                documentId,
                userId,
                file.getOriginalFilename(),
                "DOCUMENTS",
                file.getContentType(),
                file.getSize(),
                chunks.size(),
                "INDEXED",
                visibility.toUpperCase(),
                null
        );

        ragActivityLogRepository.save(
                UUID.randomUUID(),
                "DOCUMENT_INGESTED",
                "Document indexed",
                file.getOriginalFilename()
        );

        return new IngestionResult(
                documentId.toString(),
                file.getOriginalFilename(),
                chunks.size()
        );
    }

    public FolderIngestionResult ingestFolder(List<MultipartFile> files, String visibility) {

        if (files == null || files.isEmpty()) {
            return new FolderIngestionResult(
                    0,
                    0,
                    0,
                    List.of(),
                    List.of()
            );
        }

        List<IngestionResult> successfulDocuments = new ArrayList<>();
        List<FolderIngestionResult.FailedFile> failedFiles = new ArrayList<>();

        for (MultipartFile file : files) {

            String filename = file != null
                    ? file.getOriginalFilename()
                    : "unknown";

            if (file == null || file.isEmpty()) {
                failedFiles.add(
                        new FolderIngestionResult.FailedFile(
                                filename,
                                "File is empty"
                        )
                );
                continue;
            }

            try {
                IngestionResult result = ingest(file, visibility);
                successfulDocuments.add(result);

            } catch (Exception ex) {

                failedFiles.add(
                        new FolderIngestionResult.FailedFile(
                                filename,
                                ex.getMessage() != null
                                        ? ex.getMessage()
                                        : "Failed to ingest file"
                        )
                );
            }
        }

        return new FolderIngestionResult(
                files.size(),
                successfulDocuments.size(),
                failedFiles.size(),
                successfulDocuments,
                failedFiles
        );
    }

    public List<Map<String, Object>> getUserDocuments() {

        UUID userId = authenticatedUserService.getCurrentUserId();

        return documentRepository.findByUserId(userId);
    }

    public void deleteDocument(UUID documentId) {

        UUID userId = authenticatedUserService.getCurrentUserId();

        documentRepository.deleteByIdAndUserId(documentId, userId);
    }
}