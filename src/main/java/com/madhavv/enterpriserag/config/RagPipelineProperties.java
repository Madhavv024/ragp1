package com.madhavv.enterpriserag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.pipeline")
public record RagPipelineProperties(
        String chunking,
        String embeddingModel,
        String vectorStore,
        String generationModel
) {
}