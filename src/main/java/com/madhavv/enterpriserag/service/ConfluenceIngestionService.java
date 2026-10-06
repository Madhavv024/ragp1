package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.dto.*;
import com.madhavv.enterpriserag.repository.ConfluenceVectorRepository;
import com.madhavv.enterpriserag.repository.RagActivityLogRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConfluenceIngestionService {

    private final ConfluenceClient confluenceClient;
    private final ConfluenceTextExtractor textExtractor;
    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final RagActivityLogRepository ragActivityLogRepository;
    private final ConfluenceVectorRepository confluenceVectorRepository;
    private final ConfluenceUrlParser confluenceUrlParser;
    private final AuthenticatedUserService authenticatedUserService;

    public ConfluenceIngestionService(ConfluenceClient confluenceClient, ConfluenceTextExtractor textExtractor, VectorStore vectorStore, RagActivityLogRepository ragActivityLogRepository, ConfluenceVectorRepository confluenceVectorRepository, ConfluenceUrlParser confluenceUrlParser, AuthenticatedUserService authenticatedUserService) {

        this.confluenceClient = confluenceClient;
        this.textExtractor = textExtractor;
        this.vectorStore = vectorStore;
        this.ragActivityLogRepository = ragActivityLogRepository;
        this.confluenceVectorRepository = confluenceVectorRepository;
        this.confluenceUrlParser = confluenceUrlParser;
        this.authenticatedUserService = authenticatedUserService;

        this.textSplitter = TokenTextSplitter.builder()
                .withChunkSize(400)
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .build();
    }

    public int ingest(String pageId, String visbility) {

        ConfluencePage page = confluenceClient.getPage(pageId);
        return ingestPage(page, visbility);
    }

    private int ingestPage(ConfluencePage page, String visibility) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        confluenceVectorRepository.deleteByPageId(page.id(), userId);
        String content = textExtractor.extract(page.body());
        UUID documentId = UUID.randomUUID();
        OffsetDateTime ingestedAt = OffsetDateTime.now(ZoneOffset.UTC);

        Map<String, Object> metadata = Map.of(
                "documentId", documentId.toString(),
                "userId", userId.toString(),
                "filename", page.title(),
                "contentType", "confluence",
                "sourceType", "CONFLUENCE",
                "pageId", page.id(),
                "confluenceUrl", page.webUrl(),
                "ingestedAt", ingestedAt.toString(),
                "visibility", visibility.toUpperCase()
        );

        Document document = new Document(content, metadata);

        List<Document> chunks = textSplitter.apply(List.of(document));

        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put("chunkIndex", i);
        }

        vectorStore.add(chunks);

        ragActivityLogRepository.save(
                UUID.randomUUID(),
                "CONFLUENCE_INGESTED",
                "Confluence page ingested",
                page.title()
        );

        return chunks.size();
    }

    public int ingest(String pageId, ConfluenceCredentials credentials, String baseUrl, String visibility) {
        ConfluencePage page = confluenceClient.getPage(pageId, credentials, baseUrl);
        return ingestPage(page, visibility);
    }

    public ConfluenceTreeIngestionResult ingestPageTree(String rootPageId, String visibility) {

        Set<String> visitedPageIds = new HashSet<>();
        List<ConfluencePageSummary> pages = new ArrayList<>();

        collectPageTree(
                rootPageId,
                visitedPageIds,
                pages
        );

        List<ConfluenceTreeIngestionResult.PageResult> successfulPages =
                new ArrayList<>();

        List<ConfluenceTreeIngestionResult.FailedPage> failedPages =
                new ArrayList<>();

        int totalChunks = 0;

        for (ConfluencePageSummary page : pages) {

            try {
                int chunks = ingest(page.id(), visibility);
                successfulPages.add(new ConfluenceTreeIngestionResult.PageResult(page.id(), page.title(), chunks, "INGESTED"));
                totalChunks += chunks;

            } catch (Exception ex) {

                failedPages.add(
                        new ConfluenceTreeIngestionResult.FailedPage(
                                page.id(),
                                page.title(),
                                ex.getMessage() != null
                                        ? ex.getMessage()
                                        : "Failed to ingest page"
                        )
                );
            }
        }

        return new ConfluenceTreeIngestionResult(rootPageId, pages.size(), successfulPages.size(), failedPages.size(), totalChunks, successfulPages, failedPages);
    }

    private void collectPageTree(String pageId,Set<String> visitedPageIds,List<ConfluencePageSummary> pages) {
        if (!visitedPageIds.add(pageId)) {
            return;
        }
        ConfluencePage page = confluenceClient.getPage(pageId);
        pages.add(new ConfluencePageSummary(page.id(),page.title()));
        List<ConfluencePageSummary> children =confluenceClient.getChildPages(pageId);
        for (ConfluencePageSummary child : children) {
            collectPageTree(child.id(), visitedPageIds, pages);
        }
    }

    public ConfluenceTreeIngestionResult ingestFromUrl(String url, String visibility) {

        ConfluenceUrlParser.ConfluenceUrl parsed = confluenceUrlParser.parse(url);

        return switch (parsed.type()) {
            case PAGE ->
                    ingestPageTree(parsed.pageId(), visibility);
            case SPACE ->
                    ingestSpace(parsed.spaceKey(), visibility);
        };
    }

    private ConfluenceTreeIngestionResult ingestSpace(String spaceKey, String visibility) {

        var space = confluenceClient.getSpaceByKey(spaceKey);

        List<ConfluencePageSummary> pages = confluenceClient.getPagesInSpace(space.id(),visibility );

        List<ConfluenceTreeIngestionResult.PageResult> successfulPages =
                new ArrayList<>();

        List<ConfluenceTreeIngestionResult.FailedPage> failedPages =
                new ArrayList<>();

        int totalChunks = 0;

        for (ConfluencePageSummary page : pages) {

            try {

                int chunks = ingest(page.id(), visibility);

                successfulPages.add(
                        new ConfluenceTreeIngestionResult.PageResult(
                                page.id(),
                                page.title(),
                                chunks,
                                "INGESTED"
                        )
                );

                totalChunks += chunks;

            } catch (Exception ex) {

                failedPages.add(
                        new ConfluenceTreeIngestionResult.FailedPage(
                                page.id(),
                                page.title(),
                                ex.getMessage() != null
                                        ? ex.getMessage()
                                        : "Failed to ingest page"
                        )
                );
            }
        }

        return new ConfluenceTreeIngestionResult(
                "SPACE:" + space.id(),
                pages.size(),
                successfulPages.size(),
                failedPages.size(),
                totalChunks,
                successfulPages,
                failedPages
        );
    }

    public ConfluenceTreeIngestionResult ingestPageTree(String rootPageId, ConfluenceCredentials credentials, String baseUrl, String visibility) {
        Set<String> visitedPageIds = new HashSet<>();
        List<ConfluencePageSummary> pages = new ArrayList<>();

        collectPageTree(
                rootPageId,
                visitedPageIds,
                pages,
                credentials,
                baseUrl
        );

        List<ConfluenceTreeIngestionResult.PageResult> successfulPages =
                new ArrayList<>();

        List<ConfluenceTreeIngestionResult.FailedPage> failedPages =
                new ArrayList<>();

        int totalChunks = 0;

        for (ConfluencePageSummary page : pages) {
            try {
                int chunks = ingest(
                        page.id(),
                        credentials,
                        baseUrl, visibility
                );

                successfulPages.add(
                        new ConfluenceTreeIngestionResult.PageResult(
                                page.id(),
                                page.title(),
                                chunks,
                                "INGESTED"
                        )
                );

                totalChunks += chunks;

            } catch (Exception ex) {
                failedPages.add(
                        new ConfluenceTreeIngestionResult.FailedPage(
                                page.id(),
                                page.title(),
                                ex.getMessage() != null
                                        ? ex.getMessage()
                                        : "Failed to ingest page"
                        )
                );
            }
        }

        return new ConfluenceTreeIngestionResult(
                rootPageId,
                pages.size(),
                successfulPages.size(),
                failedPages.size(),
                totalChunks,
                successfulPages,
                failedPages
        );
    }

    private void collectPageTree(String pageId, Set<String> visitedPageIds, List<ConfluencePageSummary> pages, ConfluenceCredentials credentials, String baseUrl) {
        if (!visitedPageIds.add(pageId)) {
            return;
        }

        ConfluencePage page =
                confluenceClient.getPage(
                        pageId,
                        credentials,
                        baseUrl
                );

        pages.add(
                new ConfluencePageSummary(
                        page.id(),
                        page.title()
                )
        );

        List<ConfluencePageSummary> children =
                confluenceClient.getChildPages(
                        pageId,
                        credentials,
                        baseUrl
                );

        for (ConfluencePageSummary child : children) {
            collectPageTree(
                    child.id(),
                    visitedPageIds,
                    pages,
                    credentials,
                    baseUrl
            );
        }
    }

    public ConfluenceTreeIngestionResult ingestSpace(String spaceKey, ConfluenceCredentials credentials, String baseUrl, String visibility) {
        ConfluenceSpace space = confluenceClient.getSpaceByKey(
                        spaceKey,
                        credentials,
                        baseUrl
                );

        List<ConfluencePageSummary> pages =
                confluenceClient.getPagesInSpace(
                        space.id(),
                        credentials,
                        baseUrl
                );

        List<ConfluenceTreeIngestionResult.PageResult> successfulPages =
                new ArrayList<>();

        List<ConfluenceTreeIngestionResult.FailedPage> failedPages =
                new ArrayList<>();

        int totalChunks = 0;

        for (ConfluencePageSummary page : pages) {
            try {
                int chunks = ingest(
                        page.id(),
                        credentials,
                        baseUrl, visibility
                );

                successfulPages.add(
                        new ConfluenceTreeIngestionResult.PageResult(
                                page.id(),
                                page.title(),
                                chunks,
                                "INGESTED"
                        )
                );

                totalChunks += chunks;

            } catch (Exception ex) {
                failedPages.add(
                        new ConfluenceTreeIngestionResult.FailedPage(
                                page.id(),
                                page.title(),
                                ex.getMessage() != null
                                        ? ex.getMessage()
                                        : "Failed to ingest page"
                        )
                );
            }
        }

        return new ConfluenceTreeIngestionResult(
                "SPACE:" + space.id(),
                pages.size(),
                successfulPages.size(),
                failedPages.size(),
                totalChunks,
                successfulPages,
                failedPages
        );
    }
}