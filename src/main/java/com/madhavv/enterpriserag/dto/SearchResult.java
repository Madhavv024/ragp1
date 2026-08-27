package com.madhavv.enterpriserag.dto;

public record SearchResult(
        String content,
        double score,
        String documentId,
        String filename,
        Integer chunkIndex
) {
}