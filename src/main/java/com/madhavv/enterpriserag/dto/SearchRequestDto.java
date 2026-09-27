package com.madhavv.enterpriserag.dto;

import java.util.UUID;

public record SearchRequestDto(
        String question,
        String sourceType,
        String model,
        UUID conversationId
) {
}