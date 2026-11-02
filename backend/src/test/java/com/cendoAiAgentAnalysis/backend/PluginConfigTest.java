package com.cendoAiAgentAnalysis.backend;
import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.llm.ChatClient;
import com.cendoAiAgentAnalysis.backend.analyse.llm.OllamaChatClient;
import com.cendoAiAgentAnalysis.backend.analyse.naming.ClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.DimensionReducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.cendoAiAgentAnalysis.backend.analyse.embedding.CachingEmbedder;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import static org.junit.jupiter.api.Assertions.assertNotNull;

    @SpringBootTest
    class PluginConfigTest {

        @Autowired
        ChatClient chatClient;

        @Autowired
        Embedder embedder;

        @Autowired
        ReasonExtractor reasonExtractor;

        @Autowired
        DimensionReducer dimensionReducer;

        @Autowired
        Clusterer clusterer;

        @Autowired
        ClusterNamer clusterNamer;

        @Test
        void alleAnalyseBeansBliverOprettet() {
            assertNotNull(chatClient);
            assertNotNull(embedder);
            assertNotNull(reasonExtractor);
            assertNotNull(dimensionReducer);
            assertNotNull(clusterer);
            assertNotNull(clusterNamer);
        }


        @Test
        void embedderBrugerCaching() {
            assertInstanceOf(CachingEmbedder.class, embedder);
        }

        @Test
        void ollamaErValgtSomStandard() {
            assertInstanceOf(OllamaChatClient.class, chatClient);
        }
    }
