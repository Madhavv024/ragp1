package com.madhavv.enterpriserag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private final ChatClient chatClient;

    public LlmService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String ask(String question, String model) {

        return chatClient
                .prompt()
                .user(question)
                .options(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.5))
                .call()
                .content();
    }
}
