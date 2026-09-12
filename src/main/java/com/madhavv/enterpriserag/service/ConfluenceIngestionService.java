package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.ConfluencePage;
import com.madhavv.enterpriserag.repository.RagActivityLogRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConfluenceIngestionService {

    private final ConfluenceClient confluenceClient;
    private final ConfluenceTextExtractor textExtractor;
    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final RagActivityLogRepository ragActivityLogRepository;

    public ConfluenceIngestionService(
            ConfluenceClient confluenceClient,
            ConfluenceTextExtractor textExtractor,
            VectorStore vectorStore, RagActivityLogRepository ragActivityLogRepository) {

        this.confluenceClient = confluenceClient;
        this.textExtractor = textExtractor;
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

    public int ingest(String pageId) {

        ConfluencePage page = confluenceClient.getPage(pageId);

        String content = textExtractor.extract(page.body());

        UUID documentId = UUID.randomUUID();

        Map<String, Object> metadata = Map.of(
                "documentId", documentId.toString(),
                "filename", page.title(),
                "contentType", "confluence",
                "sourceType", "CONFLUENCE",
                "pageId", page.id(),
                "confluenceUrl", page.webUrl()
        );

        Document document = new Document(content, metadata);

        List<Document> chunks = textSplitter.apply(List.of(document));

        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put("chunkIndex", i);
        }

        vectorStore.add(chunks);

        ragActivityLogRepository.save(
                UUID.randomUUID(),
                "CONFLUENCE_INGESTED",
                "Confluence page ingested",
                page.title()
        );

        return chunks.size();
    }
}