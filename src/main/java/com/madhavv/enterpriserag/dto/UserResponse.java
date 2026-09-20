package com.madhavv.enterpriserag.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email
) {}