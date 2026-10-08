package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.RagDebugResponse;
import com.madhavv.enterpriserag.dto.RagResponse;
import com.madhavv.enterpriserag.dto.SearchRequestDto;
import com.madhavv.enterpriserag.dto.SearchResult;
import com.madhavv.enterpriserag.repository.ConversationRepository;
import com.madhavv.enterpriserag.repository.RagQueryLogRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final SearchService searchService;
    private final LlmService llmService;
    private final RagQueryLogRepository ragQueryLogRepository;
    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final GuardrailService guardrailService;

    public RagService(SearchService searchService, LlmService llmService, RagQueryLogRepository ragQueryLogRepository, ConversationService conversationService, ConversationRepository conversationRepository, GuardrailService guardrailService) {
        this.searchService = searchService;
        this.llmService = llmService;
        this.ragQueryLogRepository = ragQueryLogRepository;
        this.conversationService = conversationService;
        this.conversationRepository = conversationRepository;
        this.guardrailService = guardrailService;
    }

    public RagResponse ask(SearchRequestDto question) {

        UUID conversationId = question.conversationId();

        if (conversationId == null) {
            conversationId = conversationService.createConversation("New Conversation");
        } else {
            conversationService.validateOwnership(conversationId);
        }

        List<Map<String, Object>> history = conversationService.getHistory(conversationId);

        String conversationHistory = history.stream()
                .map(message -> "%s: %s".formatted(
                        message.get("role"),
                        message.get("content")
                ))
                .collect(Collectors.joining("\n"));

        conversationService.saveMessage(
                conversationId,
                "USER",
                question.question()
        );

        try{
            String sanitizedQuestion = guardrailService.sanitize(question.question());

            SearchRequestDto sanitizedRequest = new SearchRequestDto(sanitizedQuestion, question.sourceType(),
                            question.model(),
                            question.conversationId()
                    );

            List<SearchResult> results = searchService.search(sanitizedRequest);
            // 2. No documents found
            if (results == null || results.isEmpty()) {

                String answer = "I could not find the answer in the provided documents.";
                conversationService.saveMessage(
                        conversationId,
                        "ASSISTANT",
                        answer
                );

                ragQueryLogRepository.save(
                        UUID.randomUUID(),
                        question.question(),
                        question.sourceType(),
                        true
                );
                return new RagResponse(conversationId,
                        answer,
                        List.of()
                );
            }

            // 3. Build context from retrieved chunks
            String context = results.stream()
                    .map(SearchResult::content)
                    .map(guardrailService::sanitize)
                    .collect(Collectors.joining("\n\n---\n\n"));

            // 4. Create strict document-grounded prompt
            String prompt = """
                You are an enterprise document question-answering assistant.
            
                Answer the user's question using:
                1. The conversation history for context.
                2. The DOCUMENT CONTEXT as the authoritative source for factual answers.
            
                Rules:
                - Do not use your general knowledge.
                - Do not use information from outside the provided documents.
                - Do not invent facts.
                - Do not assume information that is not explicitly supported
                  by the documents.
                - Conversation history may be used to understand references
                  such as "it", "that", or "the previous topic", but factual
                  answers must still be supported by the DOCUMENT CONTEXT.
                - If the answer cannot be determined from the documents,
                  respond exactly:
                  "I could not find the answer in the provided documents."
            
                CONVERSATION HISTORY:
                %s
            
                DOCUMENT CONTEXT:
                %s
            
                USER QUESTION:
                %s
                """.formatted(
                    conversationHistory,
                    context,
                    question.question()
            );

            // 5. Send grounded prompt to DeepSeek
            String answer = llmService.ask(prompt, question.model());

            answer = guardrailService.sanitize(answer);

            List<RagResponse.Source> sources = results.stream()
                    .map(result -> new RagResponse.Source(
                            result.filename(),
                            result.score(),
                            result.documentId(),
                            result.chunkIndex(),
                            result.sourceUrl()
                    )).sorted(Comparator.comparing(entity -> entity.similarity(), Comparator.reverseOrder()))
                    .limit(3)
                    .toList();

            ragQueryLogRepository.save(
                    UUID.randomUUID(),
                    question.question(),
                    question.sourceType(),
                    true
            );

            conversationService.saveMessage(
                    conversationId,
                    "ASSISTANT",
                    answer
            );

            return new RagResponse(conversationId,answer, sources);
        }
        catch (Exception e){
            // Log failed query without hiding the original exception
            try {
                ragQueryLogRepository.save(
                        UUID.randomUUID(),
                        question.question(),
                        question.sourceType(),
                        false
                );
            } catch (Exception ignored) {
                // Telemetry failure must not replace the original RAG failure
            }

            throw e;
        }

    }

    public RagDebugResponse debug(SearchRequestDto question) {

        // 1. Retrieve relevant document chunks
        List<SearchResult> results = searchService.search(question);

        String answer;

        // 2. No documents found
        if (results == null || results.isEmpty()) {
            answer = "I could not find the answer in the provided documents.";
            return new RagDebugResponse(question.question(),question.sourceType(),null,null,answer );
        }

        // 3. Build context from retrieved chunks
        String context = results.stream()
                .map(SearchResult::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        List<RagDebugResponse.RetrievedChunk> chunks = results.stream()
                .map(result -> new RagDebugResponse.RetrievedChunk(
                        result.content(),
                        result.score(),
                        result.documentId(),
                        result.filename(),
                        result.chunkIndex()
                ))
                .toList();

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
                """.formatted(context, question.question());

        // 5. Send grounded prompt to DeepSeek
        answer = llmService.ask(prompt, question.model());

        return new RagDebugResponse( question.question(), question.sourceType(), chunks, context, answer );
    }
}