package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.RagDebugResponse;
import com.madhavv.enterpriserag.dto.RagResponse;
import com.madhavv.enterpriserag.dto.SearchRequestDto;
import com.madhavv.enterpriserag.dto.SearchResult;
import com.madhavv.enterpriserag.repository.RagQueryLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final SearchService searchService;
    private final LlmService llmService;
    private final RagQueryLogRepository ragQueryLogRepository;

    public RagService(SearchService searchService, LlmService llmService, RagQueryLogRepository ragQueryLogRepository) {
        this.searchService = searchService;
        this.llmService = llmService;
        this.ragQueryLogRepository = ragQueryLogRepository;
    }

    public RagResponse ask(SearchRequestDto question) {

        try{
            List<SearchResult> results = searchService.search(question);
            // 2. No documents found
            if (results == null || results.isEmpty()) {

                ragQueryLogRepository.save(
                        UUID.randomUUID(),
                        question.question(),
                        question.sourceType(),
                        true
                );
                return new RagResponse(
                        "I could not find the answer in the provided documents.",
                        List.of()
                );
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
                """.formatted(context, question.question());

            // 5. Send grounded prompt to DeepSeek
            String answer = llmService.ask(prompt);

            List<RagResponse.Source> sources = results.stream()
                    .map(result -> new RagResponse.Source(
                            result.filename(),
                            result.score(),
                            result.documentId(),
                            result.chunkIndex()
                    ))
                    .toList();

            ragQueryLogRepository.save(
                    UUID.randomUUID(),
                    question.question(),
                    question.sourceType(),
                    true
            );
            return new RagResponse(answer, sources);
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

        // 1. Retrieve relevant document chunks

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
        answer = llmService.ask(prompt);

        List<RagResponse.Source> sources = results.stream()
                .map(result -> new RagResponse.Source(
                        result.filename(),
                        result.score(),
                        result.documentId(),
                        result.chunkIndex()
                ))
                .toList();

        return new RagDebugResponse( question.question(), question.sourceType(), chunks, context, answer );
    }
}