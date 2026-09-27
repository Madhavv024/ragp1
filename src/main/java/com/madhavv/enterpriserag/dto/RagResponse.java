package com.madhavv.enterpriserag.dto;

import java.util.List;
import java.util.UUID;

public record RagResponse(
        UUID conversationId,
        String answer,
        List<Source> sources
) {
    public record Source(
            String filename,
            double similarity,
            String documentId,
            Integer chunkIndex,
            String sourceUrl
    ) {
    }
}
