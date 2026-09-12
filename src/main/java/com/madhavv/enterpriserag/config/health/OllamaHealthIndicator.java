package com.madhavv.enterpriserag.config.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component("ollama")
public class OllamaHealthIndicator implements HealthIndicator {

    private final RestClient restClient;

    public OllamaHealthIndicator(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public Health health() {
        try {
            restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .toBodilessEntity();

            return Health.up()
                    .withDetail("server", "Ollama")
                    .withDetail("status", "reachable")
                    .build();

        } catch (Exception ex) {
            return Health.down()
                    .withDetail("server", "Ollama")
                    .withDetail("status", "unreachable")
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}