package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.ConfluenceCredentials;
import com.madhavv.enterpriserag.dto.ConfluencePage;
import com.madhavv.enterpriserag.dto.ConfluencePageSummary;
import com.madhavv.enterpriserag.dto.ConfluenceSpace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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

    public ConfluencePage getPage(String pageId, ConfluenceCredentials credentials,String baseUrl) {

        RestClient client = createClient(credentials, baseUrl);
        Map<String, Object> response = client.get()
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
        String base = (String) links.get("base");

        return new ConfluencePage(
                id,
                title,
                content,
                base + webUrl
        );
    }

    public List<ConfluencePageSummary> getPagesInSpace(String spaceId) {

        List<ConfluencePageSummary> pages = new ArrayList<>();

        String cursor = null;

        do {
            String finalCursor = cursor;
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> {

                        var builder = uriBuilder
                                .path("/api/v2/spaces/{spaceId}/pages")
                                .queryParam("limit", 100);

                        if (finalCursor != null) {
                            builder.queryParam("cursor", finalCursor);
                        }

                        return builder.build(spaceId);
                    })
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

            if (results != null) {

                for (Map<String, Object> page : results) {

                    String id = (String) page.get("id");
                    String title = (String) page.get("title");

                    pages.add(
                            new ConfluencePageSummary(
                                    id,
                                    title
                            )
                    );
                }
            }

            cursor = extractNextCursor(response);

        } while (cursor != null);

        return pages;
    }

    private String extractNextCursor(Map<String, Object> response) {

        Map<String, Object> links =(Map<String, Object>) response.get("_links");

        if (links == null) {
            return null;
        }

        String next = (String) links.get("next");

        if (next == null || next.isBlank()) {
            return null;
        }

        int cursorIndex = next.indexOf("cursor=");
        if (cursorIndex == -1) {
            return null;
        }
        String cursor = next.substring(cursorIndex + "cursor=".length());

        int ampersandIndex = cursor.indexOf('&');

        if (ampersandIndex != -1) {
            cursor = cursor.substring(0, ampersandIndex);
        }

        return cursor;
    }

    public List<ConfluencePageSummary> getChildPages(String pageId) {

        List<ConfluencePageSummary> pages = new ArrayList<>();

        String cursor = null;

        do {

            String finalCursor = cursor;

            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> {

                        var builder = uriBuilder
                                .path("/api/v2/pages/{pageId}/children")
                                .queryParam("limit", 100);

                        if (finalCursor != null) {
                            builder.queryParam("cursor", finalCursor);
                        }

                        return builder.build(pageId);
                    })
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");

            if (results != null) {

                for (Map<String, Object> page : results) {

                    String id = (String) page.get("id");
                    String title = (String) page.get("title");

                    pages.add(
                            new ConfluencePageSummary(
                                    id,
                                    title
                            )
                    );
                }
            }

            cursor = extractNextCursor(response);

        } while (cursor != null);

        return pages;
    }

    public ConfluenceSpace getSpaceByKey(String spaceKey) {

        Map<String, Object> response = restClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/v2/spaces")
                        .queryParam("keys", spaceKey)
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException(
                    "Confluence space not found: " + spaceKey
            );
        }

        Map<String, Object> space = results.get(0);

        String id = (String) space.get("id");
        String key = (String) space.get("key");
        String name = (String) space.get("name");
        String homepageId = (String) space.get("homepageId");

        return new ConfluenceSpace(
                id,
                key,
                name,
                homepageId
        );
    }

    private RestClient createClient(ConfluenceCredentials credentials, String baseUrl) {

        String auth = Base64.getEncoder().encodeToString(
                (credentials.email() + ":" + credentials.apiToken())
                        .getBytes(StandardCharsets.UTF_8)
        );

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Basic " + auth
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    public List<ConfluencePageSummary> getChildPages(String pageId, ConfluenceCredentials credentials,String baseUrl) {
        RestClient client = createClient(credentials, baseUrl);

        List<ConfluencePageSummary> pages = new ArrayList<>();
        String cursor = null;

        do {
            String finalCursor = cursor;

            Map<String, Object> response = client.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/api/v2/pages/{pageId}/children")
                                .queryParam("limit", 100);

                        if (finalCursor != null) {
                            builder.queryParam("cursor", finalCursor);
                        }

                        return builder.build(pageId);
                    })
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");

            if (results != null) {
                for (Map<String, Object> page : results) {
                    pages.add(
                            new ConfluencePageSummary(
                                    (String) page.get("id"),
                                    (String) page.get("title")
                            )
                    );
                }
            }

            cursor = extractNextCursor(response);

        } while (cursor != null);

        return pages;
    }

    public ConfluenceSpace getSpaceByKey(String spaceKey, ConfluenceCredentials credentials,String baseUrl) {
        RestClient client = createClient(credentials, baseUrl);
        Map<String, Object> response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v2/spaces")
                        .queryParam("keys", spaceKey)
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> results =
                (List<Map<String, Object>>) response.get("results");

        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException(
                    "Confluence space not found: " + spaceKey
            );
        }

        Map<String, Object> space = results.get(0);

        return new ConfluenceSpace(
                (String) space.get("id"),
                (String) space.get("key"),
                (String) space.get("name"),
                (String) space.get("homepageId")
        );
    }

    public List<ConfluencePageSummary> getPagesInSpace(
            String spaceId,
            ConfluenceCredentials credentials,
            String baseUrl
    ) {
        RestClient client = createClient(credentials, baseUrl);

        List<ConfluencePageSummary> pages = new ArrayList<>();
        String cursor = null;

        do {
            String finalCursor = cursor;

            Map<String, Object> response = client.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/api/v2/spaces/{spaceId}/pages")
                                .queryParam("limit", 100);

                        if (finalCursor != null) {
                            builder.queryParam("cursor", finalCursor);
                        }

                        return builder.build(spaceId);
                    })
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");

            if (results != null) {
                for (Map<String, Object> page : results) {
                    pages.add(
                            new ConfluencePageSummary(
                                    (String) page.get("id"),
                                    (String) page.get("title")
                            )
                    );
                }
            }

            cursor = extractNextCursor(response);

        } while (cursor != null);

        return pages;
    }

}