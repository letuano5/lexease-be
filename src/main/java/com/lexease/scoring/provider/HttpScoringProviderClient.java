package com.lexease.scoring.provider;

import com.lexease.scoring.ScoringProperties;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpScoringProviderClient implements ScoringProviderClient {
    private final ScoringProperties properties;
    private final RestClient.Builder restClientBuilder;

    public HttpScoringProviderClient(ScoringProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public ScoringJobSubmitResponse submitJob(ScoringEvaluationJobRequest request) {
        return restClient()
                .post()
                .uri("/v1/reading-evaluations/jobs")
                .body(request)
                .retrieve()
                .body(ScoringJobSubmitResponse.class);
    }

    @Override
    public ScoringEvaluationResult evaluateSync(ScoringEvaluationJobRequest request) {
        return restClient()
                .post()
                .uri("/v1/reading-evaluations")
                .body(request)
                .retrieve()
                .body(ScoringEvaluationResult.class);
    }

    private RestClient restClient() {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SCORING_PROVIDER_FAILED, "Scoring provider is not configured");
        }
        return restClientBuilder.baseUrl(properties.baseUrl()).build();
    }
}
