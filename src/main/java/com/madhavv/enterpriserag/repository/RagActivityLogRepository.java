package com.madhavv.enterpriserag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class RagActivityLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public RagActivityLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(
            UUID id,
            String activityType,
            String description,
            String reference
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO rag_activity_log (
                    id,
                    activity_type,
                    description,
                    reference
                )
                VALUES (?, ?, ?, ?)
                """,
                id,
                activityType,
                description,
                reference
        );
    }

    public List<Activity> findRecent(int limit) {

        return jdbcTemplate.query(
                """
                SELECT
                    activity_type,
                    description,
                    reference,
                    created_at
                FROM rag_activity_log
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new Activity(
                        rs.getString("activity_type"),
                        rs.getString("description"),
                        rs.getString("reference"),
                        rs.getTimestamp("created_at")
                                .toInstant()
                                .atOffset(java.time.ZoneOffset.UTC)
                ),
                limit
        );
    }

    public record Activity(
            String activityType,
            String description,
            String reference,
            OffsetDateTime createdAt
    ) {}
}