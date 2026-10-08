package com.madhavv.enterpriserag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class DocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(
            UUID id,
            UUID userId,
            String name,
            String sourceType,
            String contentType,
            long sizeBytes,
            int chunkCount,
            String status,
            String visibility,
            String sourceUrl
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO documents (
                    id,
                    user_id,
                    name,
                    source_type,
                    content_type,
                    size_bytes,
                    chunk_count,
                    status,
                    visibility,
                    source_url
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, source_type, source_url)
                DO UPDATE SET
                    id = EXCLUDED.id,
                    name = EXCLUDED.name,
                    content_type = EXCLUDED.content_type,
                    size_bytes = EXCLUDED.size_bytes,
                    chunk_count = EXCLUDED.chunk_count,
                    status = EXCLUDED.status,
                    visibility = EXCLUDED.visibility,
                    updated_at = CURRENT_TIMESTAMP
                """,
                id,
                userId,
                name,
                sourceType,
                contentType,
                sizeBytes,
                chunkCount,
                status,
                visibility,
                sourceUrl
        );
    }

    public List<Map<String, Object>> findByUserId(UUID userId) {
        return jdbcTemplate.queryForList(
                """
                SELECT
                    id,
                    name,
                    source_type,
                    content_type,
                    size_bytes,
                    chunk_count,
                    status,
                    visibility,
                    source_url,
                    created_at,
                    updated_at
                FROM documents
                WHERE user_id = ?
                ORDER BY created_at DESC
                """,
                userId
        );
    }

    public void deleteByIdAndUserId(UUID documentId, UUID userId) {

        jdbcTemplate.update(
                """
                DELETE FROM vector_store
                WHERE metadata->>'documentId' = ?
                  AND metadata->>'userId' = ?
                """,
                documentId.toString(),
                userId.toString()
        );

        jdbcTemplate.update(
                """
                DELETE FROM documents
                WHERE id = ?
                  AND user_id = ?
                """,
                documentId,
                userId
        );
    }

    public List<Map<String, Object>> findByUserIdAndSourceType(
            UUID userId,
            String sourceType) {

        return jdbcTemplate.queryForList(
                """
                SELECT
                    id,
                    name,
                    source_type,
                    content_type,
                    size_bytes,
                    chunk_count,
                    status,
                    visibility,
                    source_url,
                    created_at,
                    updated_at
                FROM documents
                WHERE user_id = ?
                  AND source_type = ?
                ORDER BY created_at DESC
                """,
                userId,
                sourceType
        );
    }
}