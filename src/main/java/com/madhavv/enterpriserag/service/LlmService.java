package com.madhavv.enterpriserag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private final ChatClient chatClient;

    public LlmService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String ask(String question) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }
}
