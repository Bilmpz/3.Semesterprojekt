package com.cendoAiAgentAnalysis.backend;

import com.cendoAiAgentAnalysis.backend.analyse.PipelineCli;
import com.cendoAiAgentAnalysis.backend.analyse.PluginConfig;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import com.cendoAiAgentAnalysis.backend.analyse.AnalyseService;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertSame;

class PipelineWiringTest {
    @TempDir
    Path tempDir;

    @Test
    void pipelineUsesPluginsCreatedBySpring() throws Exception {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("test", properties()));
            context.register(PluginConfig.class, AnalyseService.class);
            context.refresh();
            assertPipelineBeans(context);
        }
    }

    @Test
    void cliConfigurationIsUsedInsteadOfEnvironmentDefaults() throws Exception {
        Properties properties = properties();
        properties.setProperty("ollama.llm.model", "comparison-model");
        PipelineConfig cfg = PipelineConfig.fromProperties(properties);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean("pipelineConfig", PipelineConfig.class, () -> cfg);
            context.register(PluginConfig.class, AnalyseService.class);
            context.refresh();
            assertSame(cfg, context.getBean(PipelineConfig.class));
            assertPipelineBeans(context);
        }
    }

    private Properties properties() throws Exception {
        Properties properties = new Properties();
        try (var reader = java.nio.file.Files.newBufferedReader(Path.of("config.properties"))) {
            properties.load(reader);
        }
        properties.setProperty("cache.dir", tempDir.toString());
        return properties;
    }

    private void assertPipelineBeans(AnnotationConfigApplicationContext context) {
        PipelineCli pipeline = context.getBean(PipelineCli.class);
        AnalyseService service = context.getBean(AnalyseService.class);
        for (var entry : Map.of(
                "cfg", "pipelineConfig",
                "mapper", "objectMapper",
                "extractor", "reasonExtractor",
                "embedder", "embedder",
                "reducer", "dimensionsReducer",
                "clusterer", "clusterer",
                "namer", "clusterNamer").entrySet()) {
            assertSame(context.getBean(entry.getValue()),
                    ReflectionTestUtils.getField(pipeline, entry.getKey()), entry.getKey());
            if (!entry.getKey().equals("mapper")) {
                assertSame(context.getBean(entry.getValue()), ReflectionTestUtils.getField(service,
                        entry.getKey().equals("cfg") ? "config" : entry.getKey()), entry.getKey());
            }
        }
    }
}
