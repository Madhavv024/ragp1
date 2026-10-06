package com.madhavv.enterpriserag.dto;

public record ConfluenceUserIngestionRequest(
        String url,
        String email,
        String apiToken,
        String visibility
) {}