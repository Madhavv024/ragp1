package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.SearchResult;
import org.springframework.ai.document.Document;
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

    public List<SearchResult> search(String query) {

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(5)
                .build();
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