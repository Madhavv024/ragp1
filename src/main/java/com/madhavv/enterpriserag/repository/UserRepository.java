package com.madhavv.enterpriserag.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(UUID id, String email, String passwordHash) {
        jdbcTemplate.update(
                """
                INSERT INTO app_user (
                    id,
                    email,
                    password_hash
                )
                VALUES (?, ?, ?)
                """,
                id,
                email,
                passwordHash
        );
    }

    public Optional<User> findByEmail(String email) {
        try {
            User user = jdbcTemplate.queryForObject(
                    """
                    SELECT id, email, password_hash, created_at
                    FROM app_user
                    WHERE email = ?
                    """,
                    (rs, rowNum) -> new User(
                            rs.getObject("id", UUID.class),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getObject("created_at", java.time.OffsetDateTime.class)
                    ),
                    email
            );

            return Optional.ofNullable(user);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public record User(
            UUID id,
            String email,
            String passwordHash,
            java.time.OffsetDateTime createdAt
    ) {}
}