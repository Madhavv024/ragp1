package com.madhavv.enterpriserag.config.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("pgvector")
public class PgVectorHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public PgVectorHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            Boolean installed = jdbcTemplate.queryForObject(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM pg_extension
                        WHERE extname = 'vector'
                    )
                    """,
                    Boolean.class
            );

            if (Boolean.TRUE.equals(installed)) {
                return Health.up()
                        .withDetail("extension", "vector")
                        .build();
            }

            return Health.down()
                    .withDetail("extension", "vector")
                    .withDetail("reason", "pgvector extension is not installed")
                    .build();

        } catch (Exception ex) {
            return Health.down()
                    .withDetail("extension", "vector")
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}