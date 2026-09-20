package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.SearchResult;
import org.springframework.ai.document.Document;

import com.madhavv.enterpriserag.dto.SearchRequestDto;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SearchService {

    private final VectorStore vectorStore;
    private final AuthenticatedUserService authenticatedUserService;

    public SearchService(VectorStore vectorStore, AuthenticatedUserService authenticatedUserService) {
        this.vectorStore = vectorStore;
        this.authenticatedUserService = authenticatedUserService;
    }

    public List<SearchResult> search(SearchRequestDto searchRequestDto) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(searchRequestDto.question())
                .topK(5);
        String sourceType = searchRequestDto.sourceType();
        if (sourceType != null && !sourceType.equalsIgnoreCase("ALL")) {
            builder.filterExpression(
                    "userId == '" + userId + "' && sourceType == '" + sourceType + "'"
            );
        } else {
            builder.filterExpression(
                    "userId == '" + userId + "'"
            );
        }

        SearchRequest searchRequest = builder.build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        /*System.out.println("Query: " + query);

        documents.forEach(document ->
                System.out.println(
                        "Score: " + document.getScore()
                                + " | File: " + document.getMetadata().get("filename")
                                + " | Chunk: " + document.getMetadata().get("chunkIndex")
                )
        );*/

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