package com.madhavv.enterpriserag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

@SpringBootTest
class PgVectorIntegrationTest {

    @Autowired
    private VectorStore vectorStore;

    @Test
    void shouldStoreDocument() {
        Document document = new Document(
                "Spring Boot uses dependency injection to manage application components."
        );

        vectorStore.add(List.of(document));
    }

    @Test
    void shouldRetrieveSimilarDocument() {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("How does Spring Boot manage its components?")
                        .topK(1)
                        .build()
        );

        assertFalse(results.isEmpty());

        assertEquals(
                "Spring Boot uses dependency injection to manage application components.",
                results.get(0).getText()
        );
    }
}