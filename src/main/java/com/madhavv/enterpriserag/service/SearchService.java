package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.SearchResult;
import org.springframework.ai.document.Document;

import com.madhavv.enterpriserag.dto.SearchRequestDto;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SearchService {

    private final VectorStore vectorStore;
    private final AuthenticatedUserService authenticatedUserService;

    private final int topK;
    private final double similarityThreshold;

    public SearchService(VectorStore vectorStore, AuthenticatedUserService authenticatedUserService,
            @Value("${rag.retrieval.top-k:5}") int topK, @Value("${rag.retrieval.similarity-threshold:0.70}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.authenticatedUserService = authenticatedUserService;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<SearchResult> search(SearchRequestDto searchRequestDto) {

        UUID userId = authenticatedUserService.getCurrentUserId();

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(searchRequestDto.question())
                .topK(5);

        String sourceType = searchRequestDto.sourceType();

        String accessFilter = "(userId == '" + userId + "' || visibility == 'PUBLIC')";

        if (sourceType != null && !sourceType.equalsIgnoreCase("ALL")) {

            builder.filterExpression(
                    accessFilter +
                            " && sourceType == '" + sourceType + "'"
            );

        } else {
            builder.filterExpression(accessFilter);
        }

        SearchRequest searchRequest = builder.build();

        List<Document> documents =
                vectorStore.similaritySearch(searchRequest);

        System.out.println("=== RAG RETRIEVAL BEFORE THRESHOLD ===");
        System.out.println("Query: " + searchRequestDto.question());
        System.out.println("Configured threshold: " + similarityThreshold);

        documents.forEach(document ->
                System.out.println(
                        "Score: " + document.getScore()
                                + " | File: " + document.getMetadata().get("filename")
                                + " | Visibility: " + document.getMetadata().get("visibility")
                                + " | User: " + document.getMetadata().get("userId")
                                + " | Chunk: " + document.getMetadata().get("chunkIndex")
                )
        );

        documents = documents.stream()
                .filter(document ->
                        document.getScore() != null &&
                                document.getScore() >= similarityThreshold
                )
                .toList();

        System.out.println("Chunks after threshold: " + documents.size());

        return documents.stream()
                .map(document -> new SearchResult(
                        document.getText(),
                        document.getScore(),
                        (String) document.getMetadata().get("documentId"),
                        (String) document.getMetadata().get("filename"),
                        (Integer) document.getMetadata().get("chunkIndex"),
                        (String) document.getMetadata().get("confluenceUrl")
                ))
                .toList();
    }
}