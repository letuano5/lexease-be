package com.lexease.reading.dtos.res;

import com.lexease.tts.TtsWordTiming;

public record WordTimingResponse(
        int wordIndex,
        String text,
        int startMs,
        int endMs,
        Integer startChar,
        Integer endChar
) {
    public static WordTimingResponse from(TtsWordTiming timing) {
        return new WordTimingResponse(
                timing.getWordIndex(),
                timing.getText(),
                timing.getStartMs(),
                timing.getEndMs(),
                timing.getStartChar(),
                timing.getEndChar());
    }
}
