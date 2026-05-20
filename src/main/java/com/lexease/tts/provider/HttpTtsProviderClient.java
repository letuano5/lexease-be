package com.lexease.tts.provider;

import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.tts.TtsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpTtsProviderClient implements TtsProviderClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpTtsProviderClient.class);

    private final TtsProperties properties;
    private final RestClient.Builder restClientBuilder;

    public HttpTtsProviderClient(TtsProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public TtsJobSubmitResponse submitJob(TtsJobSubmitRequest request) {
        // if (logger.isDebugEnabled()) {
            logger.info("Submitting TTS job to {}{} with body={}", properties.baseUrl(), "/v1/tts/jobs", request);
        // }
        return restClient()
                .post()
                .uri("/v1/tts/jobs")
                .body(request)
                .retrieve()
                .body(TtsJobSubmitResponse.class);
    }

    @Override
    public TtsJobStatusResponse getJobStatus(String jobId, boolean includeResult) {
        return restClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/tts/jobs/{jobId}")
                        .queryParam("includeResult", includeResult)
                        .build(jobId))
                .retrieve()
                .body(TtsJobStatusResponse.class);
    }

    @Override
    public TtsGenerationResult generateSync(TtsSyncRequest request) {
        return restClient()
                .post()
                .uri("/v1/tts/word-timings")
                .body(request)
                .retrieve()
                .body(TtsGenerationResult.class);
    }

    private RestClient restClient() {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.TTS_GENERATION_FAILED, "TTS provider is not configured");
        }
        return restClientBuilder.baseUrl(properties.baseUrl()).build();
    }
}
