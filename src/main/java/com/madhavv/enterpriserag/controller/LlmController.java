package com.madhavv.enterpriserag.controller;

import com.madhavv.enterpriserag.dto.RagDebugResponse;
import com.madhavv.enterpriserag.dto.RagRequest;
import com.madhavv.enterpriserag.dto.RagResponse;
import com.madhavv.enterpriserag.dto.SearchRequestDto;
import com.madhavv.enterpriserag.service.ConversationService;
import com.madhavv.enterpriserag.service.LlmService;
import com.madhavv.enterpriserag.service.OpenRouterService;
import com.madhavv.enterpriserag.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
* TODO
*  change the ask function response type supporting below format
* {
  "answer": "Building a production-quality enterprise RAG system requires...",
  "sources": [
    {
      "filename": "test-document.txt",
      "chunkIndex": 0,
      "score": 0.7181
    },
    {
      "filename": "test-document.txt",
      "chunkIndex": 3,
      "score": 0.6581
    }
  ]
}
*
*
* */

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmService llmService;
    private final RagService ragService;
    private final OpenRouterService openRouterService;
    private final ConversationService conversationService;

    public LlmController(LlmService llmService, RagService ragService, OpenRouterService openRouterService, ConversationService conversationService) {
        this.llmService = llmService;
        this.ragService = ragService;
        this.openRouterService = openRouterService;
        this.conversationService = conversationService;
    }

    @PostMapping("/test")
    public String test(@RequestBody Map<String, String> request) {
        return llmService.ask(request.get("question"),  request.get("model"));
    }

    @PostMapping("/ask")
    public ResponseEntity<RagResponse> ask(@RequestBody SearchRequestDto request) {
        return ResponseEntity.ok(ragService.ask(request));
    }
    @PostMapping("/debug")
    public ResponseEntity<RagDebugResponse> debug(@RequestBody SearchRequestDto request) {
        return ResponseEntity.ok(ragService.debug(request));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getUserConversations() {
        return ResponseEntity.ok(conversationService.getUserConversations());
    }

    @GetMapping("/models")
    public ResponseEntity<List<OpenRouterService.LlmModelDto>> getModels() {
        return ResponseEntity.ok(openRouterService.getModels());
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getConversationHistory(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(conversationService.getHistory(conversationId));
    }

    @PutMapping("/{conversationId}/title")
    public ResponseEntity<Void> updateConversationTitle(@PathVariable UUID conversationId, @RequestBody String title) {
        conversationService.updateConversationTitle(conversationId,title);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable UUID conversationId) {

        conversationService.deleteConversation(conversationId);

        return ResponseEntity.noContent().build();
    }
}