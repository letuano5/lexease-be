package com.lexease.progress.dtos.res;

import com.lexease.recordings.dtos.res.RecordingResponse;
import java.util.List;

public record ProgressSessionDetailResponse(
        ProgressSessionResponse session,
        List<RecordingResponse> recordings
) {
}
