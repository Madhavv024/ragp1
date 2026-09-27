package com.madhavv.enterpriserag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ConfluenceVectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public ConfluenceVectorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UUID> findIdsByPageId(String pageId) {

        return jdbcTemplate.query(
                """
                SELECT id
                FROM vector_store
                WHERE metadata->>'sourceType' = 'CONFLUENCE'
                  AND metadata->>'pageId' = ?
                """,
                (rs, rowNum) ->
                        UUID.fromString(rs.getString("id")),
                pageId
        );
    }

    public void deleteByPageId(String pageId, UUID userId) {

        jdbcTemplate.update(
                """
                DELETE FROM vector_store
                WHERE metadata->>'sourceType' = 'CONFLUENCE'
                  AND metadata->>'pageId' = ?
                  AND metadata->>'userId' = ?
                """,
                pageId,
                userId.toString()
        );
    }
}