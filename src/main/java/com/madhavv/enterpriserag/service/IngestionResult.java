package com.madhavv.enterpriserag.service;

public record IngestionResult(
        String documentId,
        String filename,
        int chunksCreated
) {
}