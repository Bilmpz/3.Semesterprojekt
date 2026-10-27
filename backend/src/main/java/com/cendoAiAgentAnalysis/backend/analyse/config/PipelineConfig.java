package com.cendoAiAgentAnalysis.backend.analyse.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * All settings for one pipeline run, read from config.properties.
 * The Hugging Face token is read from the HF_TOKEN environment variable (only needed when a provider is HUGGINGFACE).
 */
public record PipelineConfig(
        Path datasetPath,
        String split,
        PipelineMode mode,
        Provider llmProvider,
        Provider embeddingProvider,
        String ollamaUrl,
        String ollamaLlmModel,
        String ollamaEmbeddingModel,
        String huggingFaceLlmUrl,
        String huggingFaceLlmModel,
        String huggingFaceEmbeddingUrl,
        String huggingFaceEmbeddingModel,
        int embeddingBatchSize,
        boolean namingEnabled,
        boolean umapEnabled,
        int umapDimensions,
        int umapNeighbors,
        int minPoints,
        int minClusterSize,
        Path outputDir,
        Path cacheDir,
        boolean evaluationEnabled,
        List<String> compareModels,
        String hfToken) {

    public enum PipelineMode { REASON_FIRST, FULL_TRANSCRIPT }

    /** Where a model runs: locally via Ollama (free) or via the Hugging Face API (credits). */
    public enum Provider { OLLAMA, HUGGINGFACE }

    public static PipelineConfig load(Path file) throws IOException {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        }

        Provider llmProvider = Provider.valueOf(get(p, "llm.provider").toUpperCase());
        Provider embeddingProvider = Provider.valueOf(get(p, "embedding.provider").toUpperCase());

        String token = System.getenv("HF_TOKEN");
        boolean needsToken = llmProvider == Provider.HUGGINGFACE || embeddingProvider == Provider.HUGGINGFACE;
        if (needsToken && (token == null || token.isBlank())) {
            throw new IllegalStateException(
                    "Miljøvariablen HF_TOKEN er ikke sat, men config.properties bruger Hugging Face. Se README.");
        }

        return new PipelineConfig(
                Path.of(get(p, "dataset.path")),
                get(p, "dataset.split").toLowerCase(),
                PipelineMode.valueOf(get(p, "pipeline.mode").toUpperCase()),
                llmProvider,
                embeddingProvider,
                get(p, "ollama.url"),
                get(p, "ollama.llm.model"),
                get(p, "ollama.embedding.model"),
                get(p, "huggingface.llm.url"),
                get(p, "huggingface.llm.model"),
                get(p, "huggingface.embedding.url"),
                get(p, "huggingface.embedding.model"),
                Integer.parseInt(get(p, "embedding.batchSize")),
                Boolean.parseBoolean(get(p, "naming.enabled")),
                Boolean.parseBoolean(get(p, "umap.enabled")),
                Integer.parseInt(get(p, "umap.dimensions")),
                Integer.parseInt(get(p, "umap.neighbors")),
                Integer.parseInt(get(p, "hdbscan.minPoints")),
                Integer.parseInt(get(p, "hdbscan.minClusterSize")),
                Path.of(get(p, "output.dir")),
                Path.of(get(p, "cache.dir")),
                Boolean.parseBoolean(get(p, "evaluation.enabled")),
                parseList(p.getProperty("compare.models", "")),
                token == null ? null : token.strip());
    }

    /** The same settings with another local LLM. Used when several models are compared in one run. */
    public PipelineConfig withOllamaLlmModel(String model) {
        return new PipelineConfig(datasetPath, split, mode, llmProvider, embeddingProvider, ollamaUrl,
                model, ollamaEmbeddingModel, huggingFaceLlmUrl, huggingFaceLlmModel, huggingFaceEmbeddingUrl,
                huggingFaceEmbeddingModel, embeddingBatchSize, namingEnabled, umapEnabled, umapDimensions,
                umapNeighbors, minPoints, minClusterSize, outputDir, cacheDir, evaluationEnabled, compareModels,
                hfToken);
    }

    /** True when compare.models lists models to run one after another. */
    public boolean comparing() {
        return !compareModels.isEmpty();
    }

    /** The LLM that is actually used, e.g. "qwen2.5:7b (OLLAMA)". */
    public String activeLlm() {
        return (llmProvider == Provider.OLLAMA ? ollamaLlmModel : huggingFaceLlmModel) + " (" + llmProvider + ")";
    }

    /** The embedding model that is actually used, e.g. "bge-m3 (OLLAMA)". */
    public String activeEmbedding() {
        return (embeddingProvider == Provider.OLLAMA ? ollamaEmbeddingModel : huggingFaceEmbeddingModel)
                + " (" + embeddingProvider + ")";
    }

    /** "a, b,c" -> [a, b, c]; an empty value gives an empty list. */
    private static List<String> parseList(String value) {
        return Arrays.stream(value.split(","))
                .map(String::strip)
                .filter(v -> !v.isEmpty())
                .toList();
    }

    private static String get(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Mangler indstillingen '" + key + "' i config.properties");
        }
        return value.strip();
    }
}
