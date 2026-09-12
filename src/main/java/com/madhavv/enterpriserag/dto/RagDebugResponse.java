package com.madhavv.enterpriserag.dto;

import java.util.List;

public record RagDebugResponse(
        String question,
        String sourceType,
        List<RetrievedChunk> retrievedChunks,
        String context,
        String answer
) {

    public record RetrievedChunk(
            String content,
            double similarity,
            String documentId,
            String filename,
            Integer chunkIndex
    ) {
    }
}