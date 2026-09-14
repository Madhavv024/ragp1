package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.FolderIngestionResult;
import com.madhavv.enterpriserag.dto.IngestionResult;
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
    private final TokenTextSplitter textSplitter;
    private final RagActivityLogRepository ragActivityLogRepository;

    public DocumentIngestionService(VectorStore vectorStore, RagActivityLogRepository ragActivityLogRepository) {
        this.vectorStore = vectorStore;
        this.ragActivityLogRepository = ragActivityLogRepository;

        this.textSplitter = TokenTextSplitter.builder()
                .withChunkSize(400)
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .build();
    }

    public IngestionResult ingest(MultipartFile file) throws IOException {

        UUID documentId = UUID.randomUUID();

        TikaDocumentReader reader = new TikaDocumentReader(file.getResource());

        List<Document> documents = reader.get();

        documents.forEach(document ->
                System.out.println(
                        "Extracted content length: "
                                + document.getText().length()
                )
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", documentId.toString());
        metadata.put("filename", file.getOriginalFilename());
        metadata.put("contentType", file.getContentType());
        metadata.put("sourceType", "DOCUMENTS");

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

    public FolderIngestionResult ingestFolder(List<MultipartFile> files) {

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
                IngestionResult result = ingest(file);
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
}