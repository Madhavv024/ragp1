package com.madhavv.enterpriserag.dto;

import java.util.List;

public record FolderIngestionResult(
        int totalFiles,
        int successful,
        int failed,
        List<IngestionResult> documents,
        List<FailedFile> errors
) {

    public record FailedFile(
            String filename,
            String error
    ) {
    }
}