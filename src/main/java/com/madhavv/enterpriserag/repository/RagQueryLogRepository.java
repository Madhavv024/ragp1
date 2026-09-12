package com.madhavv.enterpriserag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class RagQueryLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public RagQueryLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(
            UUID id,
            String question,
            String sourceType,
            boolean success
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO rag_query_log (
                    id,
                    question,
                    source_type,
                    success
                )
                VALUES (?, ?, ?, ?)
                """,
                id,
                question,
                sourceType,
                success
        );
    }
}