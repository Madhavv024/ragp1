package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.SearchResult;
import org.springframework.ai.document.Document;

import com.madhavv.enterpriserag.dto.SearchRequestDto;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final VectorStore vectorStore;

    public SearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<SearchResult> search(SearchRequestDto searchRequestDto) {

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(searchRequestDto.question())
                .topK(5);

        String sourceType = searchRequestDto.sourceType();
        if (sourceType != null && !sourceType.equalsIgnoreCase("ALL")) {
            builder.filterExpression("sourceType == '" + sourceType + "'");
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
                        (Integer) document.getMetadata().get("chunkIndex")
                ))
                .toList();
    }
}