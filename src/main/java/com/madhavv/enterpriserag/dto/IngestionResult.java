package com.madhavv.enterpriserag.dto;

public record IngestionResult(
        String documentId,
        String filename,
        int chunksCreated
) {
}