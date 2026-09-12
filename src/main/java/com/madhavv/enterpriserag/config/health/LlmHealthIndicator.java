package com.madhavv.enterpriserag.config.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component("llmProvider")
public class LlmHealthIndicator implements HealthIndicator {

    private final RestClient restClient;
    private final String model;

    public LlmHealthIndicator(@Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String model) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.model = model;
    }

    @Override
    public Health health() {
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/models")
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Health.down()
                        .withDetail("provider", "OpenRouter")
                        .withDetail("reason", "Empty response")
                        .build();
            }

            Object data = response.get("data");

            if (!(data instanceof List<?> models)) {
                return Health.down()
                        .withDetail("provider", "OpenRouter")
                        .withDetail("reason", "Invalid models response")
                        .build();
            }

            boolean modelAvailable = models.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<?, ?>) item)
                    .anyMatch(item -> model.equals(item.get("id")));

            if (!modelAvailable) {
                return Health.down()
                        .withDetail("provider", "OpenRouter")
                        .withDetail("model", model)
                        .withDetail("reason", "Configured model not available")
                        .build();
            }

            return Health.up()
                    .withDetail("provider", "OpenRouter")
                    .withDetail("model", model)
                    .build();

        } catch (Exception ex) {
            return Health.down()
                    .withDetail("provider", "OpenRouter")
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}