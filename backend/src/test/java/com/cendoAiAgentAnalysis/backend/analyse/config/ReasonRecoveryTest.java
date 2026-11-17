package com.cendoAiAgentAnalysis.backend.analyse.config;

import com.cendoAiAgentAnalysis.backend.analyse.cache.JsonCache;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.*;
import com.cendoAiAgentAnalysis.backend.analyse.llm.ChatClient;
import com.cendoAiAgentAnalysis.backend.analyse.model.TransferReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReasonRecoveryTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void replacesRejectedCachedAnswerWithoutClearingOtherEntries() throws Exception {
        var cache = mock(JsonCache.class);
        var delegate = mock(ReasonExtractor.class);
        when(cache.get(anyString())).thenReturn(mapper.createObjectNode()
                .put("primary", "Garanti").put("explanation", "Forklaring"));
        var corrected = new TransferReason("Spørgsmål om garanti", "Spørgsmål til dækning.");
        when(delegate.extractReason("transcript")).thenReturn(corrected);
        var extractor = new CachingReasonExtractor(delegate, cache, "model|v3", mapper);
        assertFalse(extractor.isCached("transcript"));
        assertEquals(corrected, extractor.extractReason("transcript"));
        verify(delegate).extractReason("transcript");
        verify(cache).put(anyString(), any());
        verify(cache).save();
    }

    @Test
    void reusesValidCachedAnswersWithoutCallingModel() throws Exception {
        var cache = mock(JsonCache.class);
        var delegate = mock(ReasonExtractor.class);
        when(cache.get(anyString())).thenReturn(mapper.createObjectNode()
                .put("primary", "Status på reparation").put("explanation", "Spørgsmål til status."));
        var extractor = new CachingReasonExtractor(delegate, cache, "model|v3", mapper);
        assertTrue(extractor.isCached("transcript"));
        assertEquals("Status på reparation", extractor.extractReason("transcript").primary());
        verifyNoInteractions(delegate);
        verify(cache, never()).save();
    }

    @Test
    void asksForCorrectionWhenReasonBreaksValidation() throws Exception {
        var chat = mock(ChatClient.class);
        when(chat.complete(anyString(), anyString()))
                .thenReturn("{\"primary_reason\":\"Garanti\",\"explanation\":\"Spørgsmål.\"}")
                .thenReturn("{\"primary_reason\":\"Spørgsmål om garanti\",\"explanation\":\"Spørgsmål til dækning.\"}");
        assertEquals("Spørgsmål om garanti", new LlmReasonExtractor(chat, mapper)
                .extractReason("transcript").primary());
        verify(chat).complete(anyString(), contains("Dit tidligere svar blev afvist"));
        verify(chat, times(2)).complete(anyString(), anyString());
    }

    @Test
    void stopsAfterThreeAttemptsAndDoesNotCacheInvalidResult() throws Exception {
        var chat = mock(ChatClient.class);
        var cache = mock(JsonCache.class);
        when(chat.complete(anyString(), anyString())).thenReturn(
                "{\"primary_reason\":\"Garanti\",\"explanation\":\"Spørgsmål.\"}");
        var extractor = new CachingReasonExtractor(new LlmReasonExtractor(chat, mapper), cache, "v3", mapper);
        assertEquals("Garanti", extractor.extractReason("transcript").primary());
        verify(chat, times(3)).complete(anyString(), anyString());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).save();
    }
}
