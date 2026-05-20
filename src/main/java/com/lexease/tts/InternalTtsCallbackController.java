package com.lexease.tts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.tts.dtos.req.TtsCallbackRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/tts/jobs")
public class InternalTtsCallbackController {
    private final TtsCallbackVerifier callbackVerifier;
    private final TtsService ttsService;
    private final ObjectMapper objectMapper;

    public InternalTtsCallbackController(
            TtsCallbackVerifier callbackVerifier,
            TtsService ttsService,
            ObjectMapper objectMapper
    ) {
        this.callbackVerifier = callbackVerifier;
        this.ttsService = ttsService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{requestId}/callback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void callback(
            @PathVariable UUID requestId,
            @RequestHeader("X-Lexease-Timestamp") String timestamp,
            @RequestHeader("X-Lexease-Signature") String signature,
            @RequestBody String rawBody
    ) {
        callbackVerifier.verify(timestamp, signature, rawBody);
        try {
            TtsCallbackRequest request = objectMapper.readValue(rawBody, TtsCallbackRequest.class);
            ttsService.handleCallback(requestId, request);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_TTS_CALLBACK, "Invalid callback body");
        }
    }
}
