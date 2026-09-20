package com.madhavv.enterpriserag.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthenticatedUserService {

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated user not found");
        }
        Object credentials = authentication.getCredentials();
        if (!(credentials instanceof UUID userId)) {
            throw new IllegalStateException("Authenticated user ID not found");
        }
        return userId;
    }
}