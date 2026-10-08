package com.madhavv.enterpriserag.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class GuardrailService {

    private static final String REDACTED = "[REDACTED]";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern PHONE_PATTERN =
            Pattern.compile(
                    "(?<!\\d)(?:\\+91[-\\s]?)?[6-9]\\d{9}(?!\\d)"
            );

    private static final Pattern PAN_PATTERN =
            Pattern.compile(
                    "\\b[A-Z]{5}[0-9]{4}[A-Z]\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern AADHAAR_PATTERN =
            Pattern.compile(
                    "(?<!\\d)\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}(?!\\d)"
            );

    private static final Pattern CARD_PATTERN =
            Pattern.compile(
                    "(?<!\\d)(?:\\d[ -]*?){13,19}(?!\\d)"
            );

    public String sanitize(String text) {

        if (text == null || text.isBlank()) {
            return text;
        }

        String sanitized = text;

        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll(REDACTED);

        sanitized = PHONE_PATTERN.matcher(sanitized)
                .replaceAll(REDACTED);

        sanitized = PAN_PATTERN.matcher(sanitized)
                .replaceAll(REDACTED);

        sanitized = AADHAAR_PATTERN.matcher(sanitized)
                .replaceAll(REDACTED);

        sanitized = CARD_PATTERN.matcher(sanitized)
                .replaceAll(REDACTED);

        return sanitized;
    }
}