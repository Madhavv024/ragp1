package com.madhavv.enterpriserag.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record OverviewResponse(
        Metrics metrics,
        Sources sources,
        Health health,
        Pipeline pipeline,
        List<Activity> recentActivity
) {

    public record Pipeline(
            String knowledgeSources,
            String chunking,
            String embeddings,
            String vectorStore,
            String generation
    ) {
    }

    public record Metrics(
            long documents,
            long confluencePages,
            long chunks,
            long queries
    ) {
    }

    public record Sources(
            long documents,
            long confluence
    ) {
    }

    public record Health(
            String springBootApi,
            String postgresql,
            String pgvector,
            String ollama,
            String llmProvider
    ) {
    }

    public record Activity(
            String activityType,
            String description,
            String reference,
            OffsetDateTime createdAt
    ) {}
}