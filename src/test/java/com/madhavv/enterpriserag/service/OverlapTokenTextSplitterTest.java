package com.madhavv.enterpriserag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OverlapTokenTextSplitterTest {

    @Test
    void shouldCreateOverlappingChunks() {

        OverlapTokenTextSplitter splitter =
                new OverlapTokenTextSplitter();

        String text = String.join(" ", java.util.Collections.nCopies(
                1000,
                "MQTT security configuration requires TLS certificates and secure broker authentication."
        ));

        List<org.springframework.ai.document.Document> chunks =
                splitter.apply(
                        List.of(
                                new org.springframework.ai.document.Document(text)
                        )
                );

        assertTrue(chunks.size() > 1);

        System.out.println("Number of chunks: " + chunks.size());

        for (int i = 0; i < chunks.size(); i++) {

            String chunk = chunks.get(i).getText();

            System.out.println(
                    "Chunk " + i +
                            " | Characters: " + chunk.length()
            );

            System.out.println(chunk.substring(
                    0,
                    Math.min(150, chunk.length())
            ));

            System.out.println("------------------------------------------------");
        }
    }
}