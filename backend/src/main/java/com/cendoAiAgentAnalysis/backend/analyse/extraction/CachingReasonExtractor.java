package com.cendoAiAgentAnalysis.backend.analyse.extraction;

import com.cendoAiAgentAnalysis.backend.analyse.cache.JsonCache;
import com.cendoAiAgentAnalysis.backend.analyse.model.TransferReason;
import com.cendoAiAgentAnalysis.backend.analyse.util.Hashing;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cendoAiAgentAnalysis.backend.analyse.validering.AarsagValidator;

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
        JsonNode entry = cache.get(key(transcript));
        return entry != null && valid(fromCache(entry));
    }

    @Override
    public TransferReason extractReason(String transcript) throws IOException, InterruptedException {
        String key = key(transcript);
        JsonNode cached = cache.get(key);
        if (cached != null && valid(fromCache(cached))) {
            return fromCache(cached);
        }
        TransferReason reason = delegate.extractReason(transcript);
        if (!valid(reason)) {
            return reason; // Pipeline validation still reports the failed conversation.
        }
        cache.put(key, mapper.createObjectNode()
                .put("primary", reason.primary())
                .put("explanation", reason.explanation()));
        cache.save(); // save after every call so nothing is lost if the run is interrupted
        return reason;
    }

    private static TransferReason fromCache(JsonNode entry) {
        return new TransferReason(entry.path("primary").asText(""), entry.path("explanation").asText(""));
    }

    private static boolean valid(TransferReason reason) {
        return reason != null && reason.primary() != null && reason.explanation() != null
                && new AarsagValidator().validate(ReasonCleaner.clean(reason.primary())).ok();
    }

    private String key(String transcript) {
        return Hashing.sha256(keyPart, transcript);
    }
}
