package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.config.RagPipelineProperties;
import com.madhavv.enterpriserag.dto.OverviewResponse;
import com.madhavv.enterpriserag.repository.OverviewRepository;
import com.madhavv.enterpriserag.repository.RagActivityLogRepository;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OverviewService {

    private final OverviewRepository overviewRepository;
    private final HealthEndpoint healthEndpoint;
    private final RagPipelineProperties pipelineProperties;
    private final RagActivityLogRepository ragActivityLogRepository;

    public OverviewService(OverviewRepository overviewRepository, HealthEndpoint healthEndpoint, RagPipelineProperties pipelineProperties, RagActivityLogRepository ragActivityLogRepository) {
        this.overviewRepository = overviewRepository;
        this.healthEndpoint = healthEndpoint;
        this.pipelineProperties = pipelineProperties;
        this.ragActivityLogRepository = ragActivityLogRepository;
    }

    private String getStatus(String componentName) {

        var health = healthEndpoint.healthForPath(componentName);

        if (health == null) {
            return "UNKNOWN";
        }

        return health.getStatus().getCode();
    }
    public OverviewResponse getOverview() {

        long documents = overviewRepository.countDocuments();
        long confluencePages = overviewRepository.countConfluencePages();
        long chunks = overviewRepository.countChunks();
        long queries = overviewRepository.countQueries();

        OverviewResponse.Health health = new OverviewResponse.Health(
                "UP",
                getStatus("db"),
                getStatus("pgvector"),
                getStatus("ollama"),
                getStatus("llmProvider")
        );

        OverviewResponse.Pipeline pipeline = new OverviewResponse.Pipeline(
                documents + " + " + confluencePages,
                pipelineProperties.chunking(),
                pipelineProperties.embeddingModel(),
                pipelineProperties.vectorStore(),
                pipelineProperties.generationModel()
        );

        List<OverviewResponse.Activity> recentActivity = ragActivityLogRepository.findRecent(10)
                        .stream().map(activity -> new OverviewResponse.Activity(
                                activity.activityType(),
                                activity.description(),
                                activity.reference(),
                                activity.createdAt()
                        ))
                        .toList();

        return new OverviewResponse(
                new OverviewResponse.Metrics(
                        documents,
                        confluencePages,
                        chunks,
                        queries
                ),
                new OverviewResponse.Sources(
                        documents,
                        confluencePages
                ),
                health,
                pipeline,
                recentActivity
        );
    }
}