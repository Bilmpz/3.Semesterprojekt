package com.cendoAiAgentAnalysis.backend.analyse;

import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import com.cendoAiAgentAnalysis.backend.analyse.data.DatasetLoader;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonCleaner;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.model.*;
import com.cendoAiAgentAnalysis.backend.analyse.naming.ClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.DimensionReducer;
import com.cendoAiAgentAnalysis.backend.analyse.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class AnalyseService {
    private static final Logger log = LoggerFactory.getLogger(AnalyseService.class);
    private final AtomicBoolean running = new AtomicBoolean();
    private final PipelineConfig config;
    private final ReasonExtractor extractor;
    private final Embedder embedder;
    private final DimensionReducer reducer;
    private final Clusterer clusterer;
    private final ClusterNamer namer;

    public AnalyseService(PipelineConfig config, ReasonExtractor extractor, Embedder embedder,
                          DimensionReducer reducer, Clusterer clusterer, ClusterNamer namer) {
        this.config = config;
        this.extractor = extractor;
        this.embedder = embedder;
        this.reducer = reducer;
        this.clusterer = clusterer;
        this.namer = namer;
    }

    @Scheduled(cron = "${analyse.cron:0 0 2 * * *}", zone = "Europe/Copenhagen")
    public void koerNatligAnalyse() {
        try {
            // Drift må aldrig forbruge den urørte test-del.
            if (!"train".equals(config.split())) {
                log.warn("Natjob sprunget over: dataset.split skal være train");
                return;
            }
            koerAnalyse();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Natlig analyse afbrudt");
        } catch (IOException | RuntimeException e) {
            // Undtagelsestekst kan indeholde modelsvar og samtaletekst.
            log.error("Natlig analyse fejlede ({})", e.getClass().getSimpleName());
        }
    }

    public PipelineResult koerAnalyse() throws IOException, InterruptedException {
        return koerAnalyseMedDetaljer().result();
    }

    /** Embeddings og tællere fra præcis samme kørsel til E3; facit indgår aldrig her. */
    public record AnalysisRun(PipelineResult result, double[][] embeddings, int inputCount,
                              int conversationErrors, int namingErrors, boolean reductionFallback) {}

    public AnalysisRun koerAnalyseMedDetaljer() throws IOException, InterruptedException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("En analyse kører allerede");
        }
        int inputCount = 0, errors = 0, namingErrors = 0;
        boolean success = false;
        log.info("Analyse start: split={}", config.split());
        try {
            List<Conversation> input = DatasetLoader.loadTransfers(config.datasetPath(), config.split());
            inputCount = input.size();
            List<Conversation> conversations = new ArrayList<>();
            List<String> reasons = new ArrayList<>(), explanations = new ArrayList<>();
            List<double[]> vectors = new ArrayList<>();
            int dimensions = -1;
            for (Conversation conversation : input) {
                try {
                    if (conversation.transcript().isBlank()) throw new IllegalArgumentException();
                    TransferReason reason = config.mode() == PipelineConfig.PipelineMode.REASON_FIRST
                            ? extractor.extractReason(conversation.transcript())
                            : new TransferReason(conversation.transcript(), "");
                    String text = config.mode() == PipelineConfig.PipelineMode.REASON_FIRST
                            ? ReasonCleaner.clean(reason.primary()) : reason.primary();
                    if (text == null || text.isBlank() || reason.explanation() == null)
                        throw new IllegalArgumentException();
                    List<double[]> embedded = embedder.embed(List.of(text));
                    if (embedded.size() != 1) throw new IllegalArgumentException();
                    double[] vector = embedded.getFirst();
                    if (vector == null || vector.length == 0
                            || (dimensions != -1 && vector.length != dimensions)
                            || Arrays.stream(vector).anyMatch(v -> !Double.isFinite(v))
                            || Arrays.stream(vector).allMatch(v -> v == 0))
                        throw new IllegalArgumentException();
                    dimensions = vector.length;
                    // Tilføj først efter alle samtalens trin er lykkedes: indeks forbliver ens.
                    conversations.add(conversation);
                    reasons.add(text);
                    explanations.add(reason.explanation());
                    vectors.add(vector);
                } catch (IOException | RuntimeException e) {
                    errors++;
                    log.warn("Samtale sprunget over: indeks={}, fejltype={}",
                            conversations.size() + errors, e.getClass().getSimpleName());
                }
            }
            double[][] normalized = VectorUtils.l2Normalize(vectors.toArray(new double[0][]));
            Map<Integer, ClusterName> names = new TreeMap<>();
            int[] ids = new int[0];
            boolean fallback = false;
            if (!conversations.isEmpty()) {
                double[][] points;
                try {
                    points = reducer.reduce(normalized);
                } catch (RuntimeException | LinkageError e) {
                    fallback = true;
                    log.warn("Dimensionsreduktion fejlede ({}); bruger fulde embeddings", e.getClass().getSimpleName());
                    points = normalized;
                }
                ids = clusterer.cluster(points);
                if (ids.length != conversations.size()) throw new IllegalStateException("Forkert antal cluster-id'er");
                PipelineResult unnamed = new PipelineResult(conversations, reasons, explanations, ids, names);
                if (config.namingEnabled()) {
                    for (var group : unnamed.membersByCluster().entrySet()) {
                        if (group.getKey() == Clusterer.NOISE) continue;
                        try {
                            names.put(group.getKey(), Objects.requireNonNull(namer.name(
                                    group.getValue().stream().map(unnamed::displayText).toList())));
                        } catch (IOException | RuntimeException e) {
                            namingErrors++;
                            names.put(group.getKey(), new ClusterName("Klynge " + group.getKey(), ""));
                            log.warn("Navngivning fejlede for cluster {} ({})", group.getKey(), e.getClass().getSimpleName());
                        }
                    }
                }
            }
            PipelineResult result = new PipelineResult(conversations, reasons, explanations, ids, names);
            success = true;
            log.warn("Neo4j-gemning mangler: A3-skriveren er endnu en stub");
            return new AnalysisRun(result, normalized, inputCount, errors, namingErrors, fallback);
        } finally {
            log.info("Analyse slut: gennemført={}, indlæst={}, samtalefejl={}, navngivningsfejl={}",
                    success, inputCount, errors, namingErrors);
            running.set(false); // Også ved filfejl, afbrydelse og fejl i clustering.
        }
    }
}
