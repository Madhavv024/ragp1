package com.madhavv.enterpriserag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OverviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public OverviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countDocuments() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT metadata->>'documentId')
                FROM vector_store
                WHERE metadata->>'sourceType' = 'DOCUMENTS'
                """,
                Long.class
        );
    }

    public long countQueries() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM rag_query_log
                """,
                Long.class
        );
    }

    public long countConfluencePages() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT metadata->>'pageId')
                FROM vector_store
                WHERE metadata->>'sourceType' = 'CONFLUENCE'
                """,
                Long.class
        );
    }

    public long countChunks() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM vector_store
                """,
                Long.class
        );
    }
}