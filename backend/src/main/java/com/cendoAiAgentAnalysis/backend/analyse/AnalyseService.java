package com.cendoAiAgentAnalysis.backend.analyse;

import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import com.cendoAiAgentAnalysis.backend.analyse.data.DatasetLoader;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonCleaner;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.model.Conversation;
import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;
import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;
import com.cendoAiAgentAnalysis.backend.analyse.model.TransferReason;
import com.cendoAiAgentAnalysis.backend.analyse.naming.ClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.DimensionReducer;
import com.cendoAiAgentAnalysis.backend.analyse.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Gør klassen til en Spring-service, som Spring kan oprette og bruge i fx testen.
@Service
// Aktiverer Springs planlægning, så den natlige metode bliver kaldt automatisk.
@EnableScheduling
public class AnalyseService {
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


    // Skriver status og resultater i loggen, så kørslen kan følges.
    private static final Logger log =
            LoggerFactory.getLogger(AnalyseService.class);

    // Kører kl. 02 dansk tid, mens Spring Boot kører. Cron kan ændres via analyse.cron.
    @Scheduled(cron = "${analyse.cron:0 0 2 * * *}", zone = "Europe/Copenhagen")
    public void koerNatligAnalyse() {
        try {
            koerAnalyse();
        } catch (InterruptedException e) {
            // Bevarer afbrydelsessignalet, så applikationen kan lukke korrekt ned.
            Thread.currentThread().interrupt();
            log.warn("Den natlige analyse blev afbrudt", e);
        } catch (IOException | RuntimeException e) {
            log.error("Den natlige analyse fejlede", e);
        }
    }

    // Samler trinnene og returnerer resultatet, så det kan testes og sendes til A3.
    public PipelineResult koerAnalyse() throws IOException, InterruptedException {
        List<Conversation> samtaler =
                DatasetLoader.loadTransfers(
                        config.datasetPath(),
                        config.split()
                );

        log.info("Indlæst {} viderestillede samtaler", samtaler.size());

        // Stopper tidligt, hvis der ikke er nogen samtaler at analysere.
        if (samtaler.isEmpty()) {
            log.info("Ingen samtaler at analysere");
            return new PipelineResult(samtaler, List.of(), List.of(), new int[0], Map.of());
        }


        List<String> aarsager = new ArrayList<>();
        List<String> forklaringer = new ArrayList<>();

        // Udtrækker årsag og forklaring for hver samtale i datasættets rækkefølge.
        for (int i = 0; i < samtaler.size(); i++) {
            Conversation samtale = samtaler.get(i);
            TransferReason aarsag = extractor.extractReason(samtale.transcript());
            // Samtaler, årsager og forklaringer skal fortsat have samme indeks.
            aarsager.add(ReasonCleaner.clean(aarsag.primary()));
            forklaringer.add(aarsag.explanation());

            // Viser fremdriften, så længere modelkørsler kan følges i loggen.
            log.info("[{}/{}] Samtale {} – årsag: {}",
                    i + 1, samtaler.size(), samtale.id(), aarsager.getLast());
            log.info("Forklaring: {}", aarsag.explanation());
        }

        log.info("Årsagsudtræk færdigt for {} samtaler", aarsager.size());

        log.info("Laver embeddings med {}", config.activeEmbedding());
        double[][] vektorer = embedder.embed(aarsager).toArray(new double[0][]);

        // Skalerer vektorerne til længde 1, så den efterfølgende afstandsberegning
        // sammenligner deres retning frem for deres længde, ligesom i PipelineCli.
        vektorer = VectorUtils.l2Normalize(vektorer);

        log.info("Embedding færdigt: {} vektorer med {} dimensioner",
                vektorer.length, vektorer[0].length);

        String reductionDescription = reducer.description();
        log.info("Dimensionsreduktion: {}", reducer.description());
        double[][] punkter;
        try {
            punkter = reducer.reduce(vektorer);
        } catch (RuntimeException | LinkageError e) {
            // Fortsætter med de oprindelige vektorer, hvis UMAP fejler, fx ved manglende native biblioteker.
            log.warn("Dimensionsreduktion fejlede; fortsætter uden reduktion", e);
            reductionDescription = "No reduction";
            punkter = vektorer;
        }

        log.info("Dimensionsreduktion færdig: {} punkter med {} dimensioner ({})",
                punkter.length, punkter[0].length, reductionDescription);

        // HDBSCAN grupperer lignende årsager. Hvert punkt får et cluster-id; -1 betyder støj.
        log.info("Clustering: {}", clusterer.description());
        int[] clusterIds = clusterer.cluster(punkter);

        // Samler data midlertidigt, så vi kan finde eksempler fra hver gruppe til navngivning.
        Map<Integer, ClusterName> navne = new TreeMap<>();
        PipelineResult udenNavne = new PipelineResult(
                samtaler, aarsager, forklaringer, clusterIds, navne);

        // Modellen navngiver de grupper, HDBSCAN allerede har fundet, hvis navngivning er aktiveret.
        // Støj får intet gruppenavn, da disse samtaler ikke tilhører en egentlig gruppe.
        if (config.namingEnabled()) {
            for (var gruppe : udenNavne.membersByCluster().entrySet()) {
                if (gruppe.getKey() == Clusterer.NOISE) {
                    continue;
                }
                List<String> eksempler = gruppe.getValue().stream()
                        .map(udenNavne::displayText)
                        .toList();
                navne.put(gruppe.getKey(), namer.name(eksempler));
                log.info("Navngivet cluster {}: {}", gruppe.getKey(), navne.get(gruppe.getKey()));
            }
        }

        // Pakker samtaler, årsager, forklaringer, gruppe-id'er og navne i ét samlet resultat.
        PipelineResult resultat = new PipelineResult(
                samtaler, aarsager, forklaringer, clusterIds, navne);

        long antalGrupper = resultat.membersByCluster().keySet().stream()
                .filter(id -> id != Clusterer.NOISE)
                .count();
        int antalStoej = resultat.membersByCluster().getOrDefault(Clusterer.NOISE, List.of()).size();
        log.info("Analyse færdig: {} samtaler, {} grupper, {} støj",
                samtaler.size(), antalGrupper, antalStoej);
        log.warn("Resultatet er ikke gemt i Neo4j: integration med A3 mangler endnu");
        return resultat;
    }
}
