package com.madhavv.enterpriserag.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ConfluenceUrlParser {

    private static final Pattern PAGE_PATTERN = Pattern.compile(
            "^/wiki/spaces/([^/]+)/pages/(\\d+)(?:/.*)?/?$"
    );

    private static final Pattern SPACE_PATTERN = Pattern.compile(
            "^/wiki/spaces/([^/]+)/?$"
    );

    public ConfluenceUrlParser() {
    }

    public ConfluenceUrl parse(String url) {

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "Confluence URL cannot be empty"
            );
        }

        URI uri;

        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid Confluence URL"
            );
        }

        validateScheme(uri);

        String path = uri.getPath();

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid Confluence URL path"
            );
        }

        Matcher pageMatcher = PAGE_PATTERN.matcher(path);

        if (pageMatcher.matches()) {

            return new ConfluenceUrl(
                    Type.PAGE,
                    pageMatcher.group(1),
                    pageMatcher.group(2),
                    getBaseUrl(uri)
            );
        }

        Matcher spaceMatcher = SPACE_PATTERN.matcher(path);

        if (spaceMatcher.matches()) {

            return new ConfluenceUrl(
                    Type.SPACE,
                    spaceMatcher.group(1),
                    null,
                    getBaseUrl(uri)
            );
        }

        throw new IllegalArgumentException("Unsupported Confluence URL. " +"Provide a Confluence page or space URL.");
    }

    private String getBaseUrl(URI uri) {
        return uri.getScheme() + "://" + uri.getAuthority() + "/wiki";
    }

    private void validateScheme(URI uri) {

        String scheme = uri.getScheme();

        if (!"https".equalsIgnoreCase(scheme)) {

            throw new IllegalArgumentException(
                    "Confluence URL must use HTTPS"
            );
        }
    }

    public enum Type {
        PAGE,
        SPACE
    }

    public record ConfluenceUrl(
            Type type,
            String spaceKey,
            String pageId,
            String baseUrl
    ) {
    }
}