package com.lexease.tts.provider;

public interface TtsProviderClient {
    TtsJobSubmitResponse submitJob(TtsJobSubmitRequest request);

    TtsJobStatusResponse getJobStatus(String jobId, boolean includeResult);

    TtsGenerationResult generateSync(TtsSyncRequest request);
}
