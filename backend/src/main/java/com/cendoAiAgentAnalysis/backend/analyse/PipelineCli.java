package com.cendoAiAgentAnalysis.backend.analyse;

import com.cendoAiAgentAnalysis.backend.analyse.cache.JsonCache;
import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.clustering.HdbscanClusterer;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig.PipelineMode;
import com.cendoAiAgentAnalysis.backend.analyse.config.ProviderFactory;
import com.cendoAiAgentAnalysis.backend.analyse.data.DatasetLoader;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.CachingEmbedder;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.evaluation.ClusterEvaluator;
import com.cendoAiAgentAnalysis.backend.analyse.evaluation.EmbeddingEvaluator;
import com.cendoAiAgentAnalysis.backend.analyse.evaluation.EvaluationReport;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.CachingReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.LlmReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonCleaner;
import com.cendoAiAgentAnalysis.backend.analyse.llm.ChatClient;
import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;
import com.cendoAiAgentAnalysis.backend.analyse.model.Conversation;
import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;
import com.cendoAiAgentAnalysis.backend.analyse.model.TransferReason;
import com.cendoAiAgentAnalysis.backend.analyse.naming.ClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.naming.LlmClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.output.ResultWriter;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.DimensionReducer;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.NoReduction;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.UmapReducer;
import com.cendoAiAgentAnalysis.backend.analyse.util.VectorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Runs the full pipeline:
 *
 *   transcript -> (LLM: primary reason + explanation) -> embedding of the primary reason
 *              -> UMAP -> HDBSCAN -> (LLM: cluster name) -> results
 *
 * Usage: run PipelineCli in IntelliJ, optionally with the path to a config file as program argument
 * (default: config.properties). Switch between pipeline 1 and 2 with pipeline.mode in the config.
 *
 * If compare.models lists several Ollama models, the pipeline runs once per model with all other
 * settings unchanged, and a comparison table is written to results/sammenligning_<split>.csv.
 */
public class PipelineCli {

    /** The key numbers from one run, used in the model comparison table. */
    record RunSummary(String model, int conversations, int clusters, double noiseShare,
                      double ariAll, double nmiAll, double ariClustered, double nmiClustered,
                      double nearestNeighbour, double ariFixedK, double nmiFixedK,
                      double llmSecondsPerConversation, String error) {

        static RunSummary failed(String model, Throwable e) {
            return new RunSummary(model, 0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN, e.toString().replaceAll("\\s+", " "));
        }
    }

    public static void main(String[] args) throws Exception {
        PipelineConfig cfg = PipelineConfig.load(Path.of(args.length > 0 ? args[0] : "config.properties"));
        ObjectMapper mapper = new ObjectMapper();

        if (!cfg.comparing()) {
            run(cfg, mapper, cfg.outputDir().resolve(cfg.mode().name().toLowerCase() + "_" + cfg.split()));
            return;
        }

        if (cfg.llmProvider() != PipelineConfig.Provider.OLLAMA) {
            throw new IllegalStateException("compare.models virker kun med llm.provider=OLLAMA.");
        }
        List<RunSummary> summaries = new ArrayList<>();
        for (String model : cfg.compareModels()) {
            System.out.println();
            System.out.println("############ Model: " + model + " ############");
            PipelineConfig modelCfg = cfg.withOllamaLlmModel(model);
            Path outDir = cfg.outputDir().resolve(
                    cfg.mode().name().toLowerCase() + "_" + cfg.split() + "_" + slug(model));
            try {
                summaries.add(run(modelCfg, mapper, outDir));
            } catch (Exception e) { // e.g. model not pulled: keep going with the next model
                System.out.println("   Kørslen med " + model + " fejlede: " + e);
                summaries.add(RunSummary.failed(model, e));
            }
        }
        Path table = cfg.outputDir().resolve("sammenligning_" + cfg.split() + ".csv");
        writeComparison(table, summaries);
        System.out.println();
        System.out.println(comparisonText(summaries));
        System.out.println("Sammenligning gemt i: " + table.toAbsolutePath());
    }

    private static RunSummary run(PipelineConfig cfg, ObjectMapper mapper, Path outDir) throws Exception {
        ChatClient chat = ProviderFactory.chatClient(cfg, mapper);

        // 1. Load the transferred conversations
        System.out.println("1. Indlæser data fra " + cfg.datasetPath());
        List<Conversation> conversations = DatasetLoader.loadTransfers(cfg.datasetPath(), cfg.split());
        System.out.printf("   %d viderestillede samtaler (split: %s)%n", conversations.size(), cfg.split());
        if (conversations.size() < cfg.minClusterSize() * 2) {
            throw new IllegalStateException(
                    "For få samtaler til at danne klynger. Vælg et større split i config.properties.");
        }

        // 2. Decide which text represents each conversation
        long llmStart = System.nanoTime();
        Texts step2 = textsToEmbed(cfg, conversations, chat, mapper);
        List<String> texts = step2.embedded();
        double llmSeconds = (System.nanoTime() - llmStart) / 1e9;

        // 3. Embeddings
        System.out.println("3. Laver embeddings med " + cfg.activeEmbedding());
        Embedder embedder = new CachingEmbedder(
                ProviderFactory.embedder(cfg, mapper),
                new JsonCache(cfg.cacheDir().resolve("embeddings.json"), mapper),
                ProviderFactory.embeddingCacheName(cfg), mapper);
        double[][] vectors = VectorUtils.l2Normalize(embedder.embed(texts).toArray(new double[0][]));
        System.out.printf("   %d vektorer med %d dimensioner%n", vectors.length, vectors[0].length);

        // 4. Dimension reduction
        DimensionReducer reducer = cfg.umapEnabled()
                ? new UmapReducer(cfg.umapDimensions(), cfg.umapNeighbors())
                : new NoReduction();
        System.out.println("4. Dimensionsreduktion: " + reducer.description());
        double[][] points;
        try {
            points = reducer.reduce(vectors);
        } catch (Throwable e) { // UnsatisfiedLinkError if the native BLAS/ARPACK libraries are missing
            System.out.println("   UMAP kunne ikke køre (" + e + ").");
            System.out.println("   Fortsætter uden dimensionsreduktion. Se README for opsætning af UMAP.");
            reducer = new NoReduction();
            points = vectors;
        }

        // 5. Clustering
        Clusterer clusterer = new HdbscanClusterer(cfg.minPoints(), cfg.minClusterSize());
        System.out.println("5. Clustering: " + clusterer.description());
        int[] clusterIds = clusterer.cluster(points);

        // 6. Naming
        Map<Integer, ClusterName> names = new TreeMap<>();
        PipelineResult unnamed = new PipelineResult(conversations, texts, step2.explanations(), clusterIds, names);
        if (cfg.namingEnabled()) {
            System.out.println("6. Navngiver klynger med " + cfg.activeLlm());
            ClusterNamer namer = new LlmClusterNamer(chat, mapper);
            for (var entry : unnamed.membersByCluster().entrySet()) {
                if (entry.getKey() == Clusterer.NOISE) {
                    continue;
                }
                List<String> examples = entry.getValue().stream().map(unnamed::displayText).toList();
                names.put(entry.getKey(), namer.name(examples));
            }
        }
        PipelineResult result = new PipelineResult(conversations, texts, step2.explanations(), clusterIds, names);

        // 7. Evaluation (uses the answer key only now, after the clusters exist)
        EvaluationReport report = null;
        double nearestNeighbour = Double.NaN;
        EvaluationReport fixedK = null;
        if (cfg.evaluationEnabled()) {
            Map<String, String> labels = DatasetLoader.loadTransferLabels(cfg.datasetPath());
            List<String> truth = conversations.stream().map(c -> labels.getOrDefault(c.id(), "unknown")).toList();
            report = ClusterEvaluator.evaluate(clusterIds, truth);
            // Independent of HDBSCAN/UMAP: measured on the full embeddings
            nearestNeighbour = EmbeddingEvaluator.nearestNeighbourAccuracy(vectors, truth);
            int k = (int) truth.stream().distinct().count();
            fixedK = ClusterEvaluator.evaluate(EmbeddingEvaluator.averageLinkage(vectors, k), truth);
        }

        // 8. Output
        String settings = String.join("\n",
                "Pipeline:   " + cfg.mode(),
                "Split:      " + cfg.split() + " (" + conversations.size() + " samtaler)",
                "LLM:        " + cfg.activeLlm(),
                "Embedding:  " + cfg.activeEmbedding(),
                "Reduktion:  " + reducer.description(),
                "Clustering: " + clusterer.description());
        ResultWriter.write(outDir, result, settings, report, mapper);

        System.out.println();
        System.out.println(ResultWriter.summary(result, settings, report));
        if (fixedK != null) {
            String extra = String.format(
                    "=== Evaluering uden HDBSCAN (på de fulde embeddings) ===%n"
                            + "Nærmeste nabo har samme facit: %.0f%%%n"
                            + "Fast antal klynger (%d): ARI %.3f   NMI %.3f%n",
                    100 * nearestNeighbour, fixedK.clusterCount(), fixedK.ariAll(), fixedK.nmiAll());
            System.out.println(extra);
            Files.writeString(outDir.resolve("summary.txt"), "\n" + extra, StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        }
        System.out.println("Resultater gemt i: " + outDir.toAbsolutePath());

        // Includes cache hits, so the time is only comparable between models on their first run
        double perConversation = llmSeconds / conversations.size();
        int clusters = (int) Arrays.stream(clusterIds).filter(id -> id != Clusterer.NOISE).distinct().count();
        long noise = Arrays.stream(clusterIds).filter(id -> id == Clusterer.NOISE).count();
        return report == null
                ? new RunSummary(cfg.ollamaLlmModel(), conversations.size(), clusters,
                        (double) noise / conversations.size(), Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                        Double.NaN, Double.NaN, Double.NaN, perConversation, "")
                : new RunSummary(cfg.ollamaLlmModel(), conversations.size(), report.clusterCount(),
                        report.noiseShare(), report.ariAll(), report.nmiAll(), report.ariClustered(),
                        report.nmiClustered(), nearestNeighbour, fixedK.ariAll(), fixedK.nmiAll(),
                        perConversation, "");
    }

    private static void writeComparison(Path file, List<RunSummary> summaries) throws IOException {
        Files.createDirectories(file.getParent());
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM so Excel shows æ/ø/å correctly
        sb.append("model;samtaler;klynger;andel_stoej;ari_alle;nmi_alle;ari_uden_stoej;nmi_uden_stoej;"
                + "naermeste_nabo;ari_fast_k;nmi_fast_k;llm_sekunder_pr_samtale;fejl\n");
        for (RunSummary s : summaries) {
            sb.append(String.join(";", s.model(), String.valueOf(s.conversations()), String.valueOf(s.clusters()),
                    num(s.noiseShare()), num(s.ariAll()), num(s.nmiAll()), num(s.ariClustered()),
                    num(s.nmiClustered()), num(s.nearestNeighbour()), num(s.ariFixedK()), num(s.nmiFixedK()),
                    num(s.llmSecondsPerConversation()),
                    "\"" + s.error().replace("\"", "'") + "\"")).append('\n');
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String comparisonText(List<RunSummary> summaries) {
        StringBuilder sb = new StringBuilder("=== Sammenligning af modeller ===\n");
        sb.append(String.format("%-55s %7s %6s %8s %8s %8s %8s %6s %8s %8s %9s%n",
                "Model", "Klynger", "Støj", "ARI", "NMI", "ARI*", "NMI*", "Nabo", "ARI(k)", "NMI(k)", "Sek/samt"));
        for (RunSummary s : summaries) {
            if (!s.error().isEmpty()) {
                sb.append(String.format("%-55s FEJLEDE: %s%n", s.model(), s.error()));
                continue;
            }
            sb.append(String.format("%-55s %7d %5.0f%% %8.3f %8.3f %8.3f %8.3f %5.0f%% %8.3f %8.3f %9.1f%n",
                    s.model(), s.clusters(), 100 * s.noiseShare(), s.ariAll(), s.nmiAll(),
                    s.ariClustered(), s.nmiClustered(), 100 * s.nearestNeighbour(), s.ariFixedK(),
                    s.nmiFixedK(), s.llmSecondsPerConversation()));
        }
        sb.append("(Nabo = andel samtaler, hvis mest lignende årsag har samme facit. ARI(k)/NMI(k) = "
                + "fast antal klynger lig antal sande årsager. Begge er uafhængige af UMAP og HDBSCAN.)\n");
        sb.append("(* = uden støj. Sek/samt er LLM-tiden pr. samtale og er kun retvisende første gang, før cachen bruges.)\n");
        return sb.toString();
    }

    /** Comma as decimal separator, so Danish Excel reads the numbers as numbers. */
    private static String num(double v) {
        return Double.isNaN(v) ? "" : String.format(Locale.ROOT, "%.3f", v).replace('.', ',');
    }

    /** "hf.co/org/model:Q4_K_M" -> "hf.co_org_model_Q4_K_M" (safe as a folder name on Windows). */
    private static String slug(String model) {
        return model.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** The texts from step 2: what gets embedded, and the explanation shown next to it. */
    record Texts(List<String> embedded, List<String> explanations) {
    }

    /**
     * Pipeline 1 embeds the LLM's short primary reason and keeps its explanatory sentence for reading.
     * Pipeline 2 embeds the whole transcript.
     */
    private static Texts textsToEmbed(PipelineConfig cfg, List<Conversation> conversations,
                                      ChatClient chat, ObjectMapper mapper) throws Exception {
        if (cfg.mode() == PipelineMode.FULL_TRANSCRIPT) {
            System.out.println("2. Bruger hele transskriptionen (pipeline 2) - intet LLM-trin");
            List<String> transcripts = conversations.stream().map(Conversation::transcript).toList();
            return new Texts(transcripts, transcripts.stream().map(t -> "").toList());
        }

        System.out.println("2. Udtrækker viderestillingsårsager med " + cfg.activeLlm() + " (pipeline 1)");
        LlmReasonExtractor llm = new LlmReasonExtractor(chat, mapper);
        CachingReasonExtractor extractor = new CachingReasonExtractor(
                llm, new JsonCache(cfg.cacheDir().resolve("reasons.json"), mapper), llm.cacheKeyPart(), mapper);

        List<String> primaries = new ArrayList<>();
        List<String> explanations = new ArrayList<>();
        for (int i = 0; i < conversations.size(); i++) {
            Conversation c = conversations.get(i);
            boolean cached = extractor.isCached(c.transcript());
            TransferReason reason = extractor.extractReason(c.transcript());
            primaries.add(ReasonCleaner.clean(reason.primary())); // also cleans answers from the cache
            explanations.add(reason.explanation());
            System.out.printf("   [%d/%d] %s%s: %s%n        %s%n", i + 1, conversations.size(), c.id(),
                    cached ? " (cache)" : "", primaries.getLast(), reason.explanation());
        }
        return new Texts(primaries, explanations);
    }
}
