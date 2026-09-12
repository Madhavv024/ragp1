package com.madhavv.enterpriserag.dto;

public record SearchRequestDto(
        String question,
        String sourceType
) {
}