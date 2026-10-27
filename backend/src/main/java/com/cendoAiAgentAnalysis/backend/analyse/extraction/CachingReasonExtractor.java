package com.cendoAiAgentAnalysis.backend.analyse.extraction;

import com.cendoAiAgentAnalysis.backend.analyse.cache.JsonCache;
import com.cendoAiAgentAnalysis.backend.analyse.model.TransferReason;
import com.cendoAiAgentAnalysis.backend.analyse.util.Hashing;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Decorator: wraps any ReasonExtractor and remembers its answers on disk.
 * The key includes model + prompt version + transcript, so changing any of them triggers a new call.
 */
public class CachingReasonExtractor implements ReasonExtractor {

    private final ReasonExtractor delegate;
    private final JsonCache cache;
    private final String keyPart;
    private final ObjectMapper mapper;

    public CachingReasonExtractor(ReasonExtractor delegate, JsonCache cache, String keyPart, ObjectMapper mapper) {
        this.delegate = delegate;
        this.cache = cache;
        this.keyPart = keyPart;
        this.mapper = mapper;
    }

    public boolean isCached(String transcript) {
        return cache.get(key(transcript)) != null;
    }

    @Override
    public TransferReason extractReason(String transcript) throws IOException, InterruptedException {
        String key = key(transcript);
        JsonNode cached = cache.get(key);
        if (cached != null) {
            return new TransferReason(cached.path("primary").asText(""), cached.path("explanation").asText(""));
        }
        TransferReason reason = delegate.extractReason(transcript);
        cache.put(key, mapper.createObjectNode()
                .put("primary", reason.primary())
                .put("explanation", reason.explanation()));
        cache.save(); // save after every call so nothing is lost if the run is interrupted
        return reason;
    }

    private String key(String transcript) {
        return Hashing.sha256(keyPart, transcript);
    }
}
