package com.madhavv.enterpriserag.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;

@Service
public class OpenRouterService {

    private final RestClient restClient;

    public OpenRouterService(@Value("${openrouter.base-url}") String baseUrl,
            @Value("${openrouter.api-key}") String apiKey) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(30_000);
        requestFactory.setReadTimeout(30_000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer " + apiKey)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    public List<LlmModelDto> getModels() {

        OpenRouterModelsResponse response = restClient
                .get()
                .uri("/models?output_modalities=text")
                .retrieve()
                .body(OpenRouterModelsResponse.class);

        if (response == null || response.data() == null) {
            return List.of();
        }

        return response.data()
                .stream()
                .map(model -> new LlmModelDto(
                        model.id(),
                        model.name(),
                        model.canonicalSlug(),
                        model.contextLength()
                ))
                .toList();
    }

    public record LlmModelDto(
            String id,
            String name,
            String canonicalSlug,
            Long contextLength
    ) {}

    public record OpenRouterModelsResponse(
            List<OpenRouterModel> data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenRouterModel(
            String id,
            String name,

            @JsonProperty("canonical_slug")
            String canonicalSlug,

            @JsonProperty("context_length")
            Long contextLength
    ) {}
}