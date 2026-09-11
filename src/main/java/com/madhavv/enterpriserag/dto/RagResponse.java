package com.madhavv.enterpriserag.dto;

import java.util.List;

public record RagResponse(
        String answer,
        List<Source> sources
) {
    public record Source(
            String filename,
            double similarity,
            String documentId,
            Integer chunkIndex
    ) {
    }
}
