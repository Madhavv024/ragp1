package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.SearchResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final SearchService searchService;
    private final LlmService llmService;

    public RagService(SearchService searchService,
                      LlmService llmService) {
        this.searchService = searchService;
        this.llmService = llmService;
    }

    public String ask(String question) {

        // 1. Retrieve relevant document chunks
        List<SearchResult> results = searchService.search(question);

        // 2. No documents found
        if (results == null || results.isEmpty()) {
            return "I could not find the answer in the provided documents.";
        }

        // 3. Build context from retrieved chunks
        String context = results.stream()
                .map(SearchResult::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 4. Create strict document-grounded prompt
        String prompt = """
                You are an enterprise document question-answering assistant.

                Answer the user's question ONLY using the information
                contained in the DOCUMENT CONTEXT below.

                Rules:
                - Do not use your general knowledge.
                - Do not use information from outside the provided documents.
                - Do not invent facts.
                - Do not assume information that is not explicitly supported
                  by the documents.
                - If the answer cannot be determined from the documents,
                  respond exactly:
                  "I could not find the answer in the provided documents."

                DOCUMENT CONTEXT:
                %s

                USER QUESTION:
                %s
                """.formatted(context, question);

        // 5. Send grounded prompt to DeepSeek
        return llmService.ask(prompt);
    }
}