package com.cendoAiAgentAnalysis.backend.analyse;

// ID:          A2
// Delsystem:   Analyse
// Hvad:        Erstatter ProviderFactory i Spring: laver plug-ins (ChatClient, Embedder, DimensionReducer, Clusterer, ClusterNamer) som @Bean ud fra application.properties.
// Se docs/Byggeplan_v0.6.docx


import com.cendoAiAgentAnalysis.backend.analyse.cache.JsonCache;
import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.clustering.HdbscanClusterer;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import com.cendoAiAgentAnalysis.backend.analyse.config.ProviderFactory;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.CachingEmbedder;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.CachingReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.LlmReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.llm.ChatClient;
import com.cendoAiAgentAnalysis.backend.analyse.naming.ClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.naming.LlmClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.DimensionReducer;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.NoReduction;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.UmapReducer;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import java.util.Properties;


@Configuration //"omkapsler" den del som skal konfigureres af Spring

public class PluginConfig {
    @Bean
    public PipelineCli pipeline(PipelineConfig cfg, ObjectMapper mapper, ReasonExtractor extractor,
                                Embedder embedder, DimensionReducer reducer, Clusterer clusterer,
                                ClusterNamer namer) {
        return new PipelineCli(cfg, mapper, extractor, embedder, reducer, clusterer, namer);
    }


    @Bean //Konfigurationen
    @ConditionalOnMissingBean(PipelineConfig.class)
    public PipelineConfig pipelineConfig(Environment env) {
        Properties p = new Properties();

        String[] keys = { //indstillinger i application.properties
                "dataset.path", "dataset.split", "pipeline.mode",
                "llm.provider", "embedding.provider",
                "ollama.url", "ollama.llm.model", "ollama.embedding.model",
                "huggingface.llm.url", "huggingface.llm.model",
                "huggingface.embedding.url", "huggingface.embedding.model",
                "embedding.batchSize", "naming.enabled",
                "umap.enabled", "umap.dimensions", "umap.neighbors",
                "hdbscan.minPoints", "hdbscan.minClusterSize",
                "output.dir", "cache.dir", "evaluation.enabled",
                "compare.models"
        };

        for (String key: keys){
            String value = env.getProperty(key);
            if (value !=null){
                p.setProperty(key, value);
            }
        }

        return PipelineConfig.fromProperties(p);
    }

    @Bean //JSON-håndtering
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    @Bean //AI-kommunikation
    public ChatClient chatClient(PipelineConfig cfg, ObjectMapper mapper){ //
        return ProviderFactory.chatClient(cfg, mapper);
    }

    @Bean //årsagsudtrækker
    public ReasonExtractor reasonExtractor(PipelineConfig cfg, ObjectMapper mapper, ChatClient chat) throws IOException {
        LlmReasonExtractor llm = new LlmReasonExtractor(chat, mapper);
        JsonCache cache = new JsonCache(cfg.cacheDir().resolve("reasons.json"), mapper);
        CachingReasonExtractor extractor = new CachingReasonExtractor(llm, cache, llm.cacheKeyPart(), mapper);

        return extractor;
    }

    @Bean //embedder
    public Embedder embedder(PipelineConfig cfg, ObjectMapper mapper)
            throws IOException {

        return new CachingEmbedder(
                ProviderFactory.embedder(cfg, mapper),
                new JsonCache(
                        cfg.cacheDir().resolve("embeddings.json"),
                        mapper
                ),
                ProviderFactory.embeddingCacheName(cfg),
                mapper
        );
    }

    @Bean //dimensionsreduktion
    public DimensionReducer dimensionsReducer(PipelineConfig cfg) {

        if (cfg.umapEnabled()) {
            return new UmapReducer(cfg.umapDimensions(), cfg.umapNeighbors());
        } else {
            return new NoReduction();
        }
    }

    @Bean //clustering
    public Clusterer clusterer(PipelineConfig cfg) {
        return new HdbscanClusterer(cfg.minPoints(), cfg.minClusterSize());
    }

    @Bean //navngivning
    public ClusterNamer clusterNamer(ChatClient chat, ObjectMapper mapper){
            return new LlmClusterNamer(chat, mapper);
    }

}