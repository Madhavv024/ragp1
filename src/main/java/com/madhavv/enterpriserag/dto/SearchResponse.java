package com.madhavv.enterpriserag.dto;

import java.util.List;

public record SearchResponse(
        List<SearchResult> results
) {
}