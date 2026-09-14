package com.madhavv.enterpriserag.dto;

import java.util.List;

public record ConfluenceTreeIngestionResult(
        String rootPageId,
        int totalPages,
        int successful,
        int failed,
        int totalChunks,
        List<PageResult> pages,
        List<FailedPage> errors
) {

    public record PageResult(
            String pageId,
            String title,
            int chunks,
            String status
    ) {
    }

    public record FailedPage(
            String pageId,
            String title,
            String error
    ) {
    }
}