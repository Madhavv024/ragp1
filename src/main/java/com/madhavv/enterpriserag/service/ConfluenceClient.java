package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.ConfluencePage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class ConfluenceClient {

    private final RestClient restClient;

    public ConfluenceClient(
            @Value("${confluence.base-url}") String baseUrl,
            @Value("${confluence.email}") String email,
            @Value("${confluence.api-token}") String apiToken) {

        String credentials = Base64.getEncoder()
                .encodeToString(
                        (email + ":" + apiToken)
                                .getBytes(StandardCharsets.UTF_8)
                );

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Basic " + credentials
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    public ConfluencePage getPage(String pageId) {

        Map<String, Object> response = restClient.get()
                .uri("/api/v2/pages/{pageId}?body-format=storage", pageId)
                .retrieve()
                .body(Map.class);

        String id = (String) response.get("id");
        String title = (String) response.get("title");

        Map<String, Object> body = (Map<String, Object>) response.get("body");

        Map<String, Object> storage = (Map<String, Object>) body.get("storage");

        String content = (String) storage.get("value");

        Map<String, Object> links = (Map<String, Object>) response.get("_links");

        String webUrl = (String) links.get("webui");

        String baseUrl = (String) links.get("base");

        return new ConfluencePage(
                id,
                title,
                content,
                baseUrl + webUrl
        );
    }
}