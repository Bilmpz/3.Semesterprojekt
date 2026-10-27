package com.cendoAiAgentAnalysis.backend.analyse.config;

import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.HuggingFaceEmbedder;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.OllamaEmbedder;
import com.cendoAiAgentAnalysis.backend.analyse.http.JsonHttpClient;
import com.cendoAiAgentAnalysis.backend.analyse.llm.ChatClient;
import com.cendoAiAgentAnalysis.backend.analyse.llm.HuggingFaceChatClient;
import com.cendoAiAgentAnalysis.backend.analyse.llm.OllamaChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Factory: creates the right implementation for each provider chosen in config.properties.
 * This is the only place that knows about Ollama vs Hugging Face; the rest of the pipeline
 * only sees the ChatClient and Embedder interfaces.
 */
public final class ProviderFactory {

    // Local models on a laptop CPU can be slow, so Ollama gets a generous timeout
    private static final Duration OLLAMA_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration API_TIMEOUT = Duration.ofMinutes(3);

    private ProviderFactory() {
    }

    public static ChatClient chatClient(PipelineConfig cfg, ObjectMapper mapper) {
        return switch (cfg.llmProvider()) {
            case OLLAMA -> new OllamaChatClient(
                    new JsonHttpClient(null, OLLAMA_TIMEOUT, mapper), cfg.ollamaUrl(), cfg.ollamaLlmModel());
            case HUGGINGFACE -> new HuggingFaceChatClient(
                    new JsonHttpClient(cfg.hfToken(), API_TIMEOUT, mapper),
                    cfg.huggingFaceLlmUrl(), cfg.huggingFaceLlmModel());
        };
    }

    public static Embedder embedder(PipelineConfig cfg, ObjectMapper mapper) {
        return switch (cfg.embeddingProvider()) {
            case OLLAMA -> new OllamaEmbedder(
                    new JsonHttpClient(null, OLLAMA_TIMEOUT, mapper),
                    cfg.ollamaUrl(), cfg.ollamaEmbeddingModel(), cfg.embeddingBatchSize());
            case HUGGINGFACE -> new HuggingFaceEmbedder(
                    new JsonHttpClient(cfg.hfToken(), API_TIMEOUT, mapper),
                    cfg.huggingFaceEmbeddingUrl(), cfg.huggingFaceEmbeddingModel(), cfg.embeddingBatchSize());
        };
    }

    /** Model name used in the embedding cache key (so vectors from different models are never mixed). */
    public static String embeddingCacheName(PipelineConfig cfg) {
        return cfg.embeddingProvider() + ":" + (cfg.embeddingProvider() == PipelineConfig.Provider.OLLAMA
                ? cfg.ollamaEmbeddingModel() : cfg.huggingFaceEmbeddingModel());
    }
}
