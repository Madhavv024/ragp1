package com.madhavv.enterpriserag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findConversationsByUserId(UUID userId) {
        return jdbcTemplate.queryForList(
                """
                SELECT id, title, created_at, updated_at
                FROM conversations
                WHERE user_id = ?
                ORDER BY updated_at DESC
                """,
                userId
        );
    }

    public boolean existsByIdAndUserId(UUID conversationId, UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM conversations
                WHERE id = ?
                  AND user_id = ?
                """,
                Integer.class, conversationId, userId
        );
        return count != null && count > 0;
    }

    public void createConversation(UUID conversationId, UUID userId, String title) {
        jdbcTemplate.update(
                """
                INSERT INTO conversations
                    (id, user_id, title, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                conversationId,
                userId,
                title
        );
    }

    public void saveMessage(UUID messageId, UUID conversationId, String role, String content) {
        jdbcTemplate.update(
                """
                INSERT INTO conversation_messages
                    (id, conversation_id, role, content, created_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                messageId, conversationId, role, content
        );

        jdbcTemplate.update(
                """
                UPDATE conversations
                SET updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                conversationId
        );
    }

    public List<Map<String, Object>> findMessagesByConversationId(UUID conversationId, int limit) {
        return jdbcTemplate.queryForList(
                """
                SELECT id, conversation_id, role, content, created_at
                FROM conversation_messages
                WHERE conversation_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                conversationId, limit
        );
    }

    public void updateConversationTitle(UUID conversationId, UUID userId, String title) {
        jdbcTemplate.update(
                """
                UPDATE conversations
                SET title = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND user_id = ?
                """,
                title,
                conversationId,
                userId
        );
    }
}
