package com.madhavv.enterpriserag.controller;

import com.madhavv.enterpriserag.dto.RagRequest;
import com.madhavv.enterpriserag.dto.RagResponse;
import com.madhavv.enterpriserag.service.LlmService;
import com.madhavv.enterpriserag.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

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

    public LlmController(LlmService llmService,  RagService ragService) {
        this.llmService = llmService;
        this.ragService = ragService;
    }

    @PostMapping("/test")
    public String test(@RequestBody Map<String, String> request) {
        return llmService.ask(request.get("question"));
    }

    @PostMapping("/ask")
    public ResponseEntity<RagResponse> ask(@RequestBody RagRequest request) {
        return ResponseEntity.ok(ragService.ask(request.question()));
    }
}