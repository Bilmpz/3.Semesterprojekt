package com.cendoAiAgentAnalysis.backend;

import com.cendoAiAgentAnalysis.backend.analyse.*;
import com.cendoAiAgentAnalysis.backend.analyse.data.DatasetLoader;
import com.cendoAiAgentAnalysis.backend.analyse.evaluation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EvalueringTest {
    @Test
    void kendtGrupperingGiverPerfekteScores() {
        var labels = List.of("a", "a", "b", "b");
        double[][] vectors = {{1, 0}, {1, 0}, {0, 1}, {0, 1}};
        var report = ClusterEvaluator.evaluate(new int[]{4, 4, 7, 7}, labels);
        assertEquals(1, report.ariAll(), 1e-9);
        assertEquals(1, report.nmiAll(), 1e-9);
        assertEquals(2, report.clusterCount());
        assertEquals(0, report.noiseShare());
        assertEquals(1, EmbeddingEvaluator.nearestNeighbourAccuracy(vectors, labels));
        assertEquals(1, ClusterEvaluator.evaluate(EmbeddingEvaluator.averageLinkage(vectors, 2), labels).ariAll(), 1e-9);
        assertEquals(0.25, ClusterEvaluator.evaluate(new int[]{0, 0, 1, -1}, labels).noiseShare());
    }

    @Test
    @EnabledIfSystemProperty(named = "evaluation.final", matches = "true")
    void endeligEvalueringEnGang() throws Exception {
        Properties settings = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("config.properties"))) { settings.load(reader); }
        Path output = Path.of(settings.getProperty("output.dir"), "e3-final");
        Files.createDirectories(output.getParent());
        // Atomisk: afbryd også ved et tidligere uafsluttet forsøg.
        Files.createDirectory(output);
        Files.writeString(output.resolve("STARTED.txt"), "Start: " + java.time.Instant.now());
        settings.setProperty("analyse.cron", "-");
        Properties snapshot = new Properties();
        for (String key : List.of("pipeline.mode", "llm.provider", "embedding.provider", "ollama.llm.model",
                "ollama.embedding.model", "umap.enabled", "umap.dimensions", "umap.neighbors",
                "hdbscan.minPoints", "hdbscan.minClusterSize", "dataset.path")) {
            snapshot.setProperty(key, settings.getProperty(key));
        }
        try (var writer = Files.newBufferedWriter(output.resolve("settings.properties"))) {
            snapshot.store(writer, "Faste indstillinger for train og test");
        }
        Map<String, Object> reports = new LinkedHashMap<>();
        for (String split : List.of("train", "test")) {
            settings.setProperty("dataset.split", split);
            try (var context = new AnnotationConfigApplicationContext()) {
                context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("evaluation", settings));
                context.register(PluginConfig.class, AnalyseService.class);
                context.refresh();
                var run = context.getBean(AnalyseService.class).koerAnalyseMedDetaljer();
                var result = run.result();
                assertFalse(result.conversations().isEmpty(), "Ingen behandlede samtaler i " + split);
                assertFalse(run.reductionFallback(), "UMAP fejlede; forsøget kan ikke bruges som endelig evaluering");
                var truth = DatasetLoader.loadTransferLabels(Path.of(settings.getProperty("dataset.path")));
                var labels = result.conversations().stream().map(c -> truth.get(c.id())).toList();
                assertTrue(labels.stream().allMatch(label -> label != null && !label.isBlank()), "Facit mangler");
                Map<String, Object> report = new LinkedHashMap<>();
                report.put("inputCount", run.inputCount());
                report.put("processed", result.conversations().size());
                report.put("conversationErrors", run.conversationErrors());
                report.put("namingErrors", run.namingErrors());
                report.put("clustering", ClusterEvaluator.evaluate(result.clusterIds(), labels));
                report.put("nearestNeighbourAccuracy", EmbeddingEvaluator.nearestNeighbourAccuracy(run.embeddings(), labels));
                report.put("fixedK", ClusterEvaluator.evaluate(EmbeddingEvaluator.averageLinkage(run.embeddings(),
                        (int) labels.stream().distinct().count()), labels));
                reports.put(split, report);
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve(split + ".json").toFile(), report);
            }
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("comparison.json").toFile(), reports);
        Files.writeString(output.resolve("COMPLETED.txt"), "Slut: " + java.time.Instant.now());
    }
}
