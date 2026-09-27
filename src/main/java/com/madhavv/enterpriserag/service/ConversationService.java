package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.repository.ConversationRepository;
import com.madhavv.enterpriserag.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Value("${conversation.history.max-messages}")
    private int maxHistoryMessages;

    public ConversationService(ConversationRepository conversationRepository, AuthenticatedUserService authenticatedUserService) {
        this.conversationRepository = conversationRepository;
        this.authenticatedUserService = authenticatedUserService;
    }


    public void validateOwnership(UUID conversationId) {
        UUID userId = authenticatedUserService.getCurrentUserId();

        boolean owned = conversationRepository.existsByIdAndUserId(
                conversationId,
                userId
        );

        if (!owned) {
            throw new IllegalArgumentException("Conversation not found");
        }
    }

    public UUID createConversation(String title) {
        UUID conversationId = UUID.randomUUID();
        UUID userId = authenticatedUserService.getCurrentUserId();
        conversationRepository.createConversation(conversationId, userId, title);
        return conversationId;
    }

    public List<Map<String, Object>> getHistory(UUID conversationId) {
        List<Map<String, Object>> history = conversationRepository.findMessagesByConversationId(conversationId, maxHistoryMessages );
        return history.reversed();
    }

    public void saveMessage(UUID conversationId, String role, String content) {
        conversationRepository.saveMessage(UUID.randomUUID(), conversationId, role, content);
    }

    public List<Map<String, Object>> getUserConversations() {
        UUID userId = authenticatedUserService.getCurrentUserId();
        return conversationRepository.findConversationsByUserId(userId);
    }

    public void updateConversationTitle(UUID conversationId, String title) {
        UUID userId = authenticatedUserService.getCurrentUserId();

        conversationRepository.updateConversationTitle(
                conversationId,
                userId,
                title
        );
    }
}
