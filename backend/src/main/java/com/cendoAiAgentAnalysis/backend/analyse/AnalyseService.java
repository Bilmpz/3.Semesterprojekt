package com.cendoAiAgentAnalysis.backend.analyse;

import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import com.cendoAiAgentAnalysis.backend.analyse.data.DatasetLoader;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.evaluation.SilhouetteBeregner;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonCleaner;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.model.*;
import com.cendoAiAgentAnalysis.backend.analyse.naming.ClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.DimensionReducer;
import com.cendoAiAgentAnalysis.backend.analyse.util.VectorUtils;
import com.cendoAiAgentAnalysis.backend.analyse.validering.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.EnableScheduling;

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
            boolean reasonFirst = config.mode() == PipelineConfig.PipelineMode.REASON_FIRST;
            // A5: ét filter efter hvert trin. Fejler et filter, springes samtalen over og tælles som fejl.
            IndlaesningValidator indlaesning = new IndlaesningValidator();
            AarsagValidator aarsag = new AarsagValidator();
            EmbeddingValidator embedding = null; // Laves, når vi kender dimensionen fra den første vektor
            for (Conversation conversation : input) {
                try {
                    indlaesning.kraevGyldig(conversation.transcript());
                    TransferReason reason = reasonFirst
                            ? extractor.extractReason(conversation.transcript())
                            : new TransferReason(conversation.transcript(), "");
                    String text = reasonFirst
                            ? aarsag.kraevGyldig(ReasonCleaner.clean(reason.primary())) : reason.primary();
                    if (text == null || text.isBlank() || reason.explanation() == null)
                        throw new IllegalArgumentException();
                    List<double[]> embedded = embedder.embed(List.of(text));
                    if (embedded.size() != 1) throw new IllegalArgumentException();
                    double[] vector = embedded.getFirst();
                    if (embedding == null && vector != null) embedding = new EmbeddingValidator(vector.length);
                    if (embedding == null) throw new IllegalArgumentException();
                    embedding.kraevGyldig(vector); // Tom, forkert dimension, NaN/uendelig eller nulvektor
                    dimensions = vector.length;
                    // Tilføj først efter alle samtalens trin er lykkedes: indeks forbliver ens.
                    conversations.add(conversation);
                    reasons.add(text);
                    explanations.add(reason.explanation());
                    vectors.add(vector);
                } catch (StepFailedException e) {
                    errors++;
                    // Kun trin og fejlkode i loggen, aldrig samtaletekst
                    log.warn("Samtale sprunget over: indeks={}, trin={}, fejlkode={}",
                            conversations.size() + errors, e.trin(), e.fejlKode());
                } catch (IOException | RuntimeException e) {
                    errors++;
                    log.warn("Samtale sprunget over: indeks={}, fejltype={}",
                            conversations.size() + errors, e.getClass().getSimpleName());
                }
            }
            double[][] normalized = VectorUtils.l2Normalize(vectors.toArray(new double[0][]));
            Map<Integer, ClusterName> names = new TreeMap<>();
            int[] ids = new int[0];
            Map<Integer, Double> silhouetter = Map.of(); // E4
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
                // A5: reduktionen skal give én række pr. samtale, alle med samme antal dimensioner og uden NaN.
                // Antallet af dimensioner tages fra første række, så et andet reduktions-plug-in også virker (designmål 2).
                // Fejl her stopper kørslen.
                int forventetDim = points.length > 0 && points[0] != null ? points[0].length : 0;
                new ReduktionValidator(conversations.size(), forventetDim).kraevGyldig(points);
                ids = clusterer.cluster(points);
                // A5: én label pr. samtale. Forkert antal stopper kørslen (ClusteringValidator med 1 cluster og 100 % støj tilladt).
                new ClusteringValidator(conversations.size(), 1, 1.0).kraevGyldig(ids);
                // Få clusters eller meget støj er et kvalitetsproblem, ikke en fejl: kun en advarsel i loggen.
                ValidationResult kvalitet = new ClusteringValidator(conversations.size()).validate(ids);
                if (!kvalitet.ok()) log.warn("Clustering-advarsel: {} ({})", kvalitet.fejlKode(), kvalitet.besked());
                // E4: clusterkvalitet uden facit, beregnet på de samme punkter, som blev clustered
                silhouetter = SilhouetteBeregner.perCluster(points, ids);
                log.info("Silhouette samlet={}, pr. cluster={}",
                        String.format(Locale.ROOT, "%.3f", SilhouetteBeregner.samlet(points, ids)), silhouetter);
                PipelineResult unnamed = new PipelineResult(conversations, reasons, explanations, ids, names);
                if (config.namingEnabled()) {
                    NavngivningValidator navngivning = new NavngivningValidator();
                    for (var group : unnamed.membersByCluster().entrySet()) {
                        if (group.getKey() == Clusterer.NOISE) continue;
                        try {
                            ClusterName navn = Objects.requireNonNull(namer.name(
                                    group.getValue().stream().map(unnamed::displayText).toList()));
                            navngivning.kraevGyldig(navn.name()); // A5: ugyldigt navn -> standardnavn nedenfor
                            names.put(group.getKey(), navn);
                        } catch (IOException | RuntimeException e) {
                            namingErrors++;
                            names.put(group.getKey(), new ClusterName("Klynge " + group.getKey(), ""));
                            log.warn("Navngivning fejlede for cluster {} ({})", group.getKey(), e.getClass().getSimpleName());
                        }
                    }
                    // A5: til sidst et samlet tjek for manglende navne og dubletter (kun advarsel)
                    Map<Integer, String> navne = new TreeMap<>();
                    names.forEach((id, n) -> navne.put(id, n.name()));
                    Set<Integer> forventede = new TreeSet<>(unnamed.membersByCluster().keySet());
                    forventede.remove(Clusterer.NOISE);
                    ValidationResult alleNavne = NavngivningValidator.tjekAlleNavne(navne, forventede);
                    if (!alleNavne.ok()) log.warn("Navngivning-advarsel: {} ({})", alleNavne.fejlKode(), alleNavne.besked());
                }
            }
            PipelineResult result = new PipelineResult(conversations, reasons, explanations, ids, names, silhouetter);
            success = true;
            log.debug("Analyseresultat klar; AnalyseOgGem gemmer resultatet i Neo4j");
            return new AnalysisRun(result, normalized, inputCount, errors, namingErrors, fallback);
        } finally {
            log.info("Analyse slut: gennemført={}, indlæst={}, samtalefejl={}, navngivningsfejl={}",
                    success, inputCount, errors, namingErrors);
            running.set(false); // Også ved filfejl, afbrydelse og fejl i clustering.
        }
    }
}
