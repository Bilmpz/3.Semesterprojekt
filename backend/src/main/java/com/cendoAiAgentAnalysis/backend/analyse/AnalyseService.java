package com.cendoAiAgentAnalysis.backend.analyse;

import com.cendoAiAgentAnalysis.backend.analyse.cache.JsonCache;
import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.clustering.HdbscanClusterer;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import com.cendoAiAgentAnalysis.backend.analyse.config.ProviderFactory;
import com.cendoAiAgentAnalysis.backend.analyse.data.DatasetLoader;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.CachingEmbedder;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.LlmReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.CachingReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonCleaner;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.llm.ChatClient;
import com.cendoAiAgentAnalysis.backend.analyse.model.Conversation;
import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;
import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;
import com.cendoAiAgentAnalysis.backend.analyse.model.TransferReason;
import com.cendoAiAgentAnalysis.backend.analyse.naming.ClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.naming.LlmClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.DimensionReducer;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.NoReduction;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.UmapReducer;
import com.cendoAiAgentAnalysis.backend.analyse.util.VectorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Gør klassen til en Spring-service, som Spring kan oprette og bruge i fx testen.
@Service
// Aktiverer Springs planlægning, så den natlige metode bliver kaldt automatisk.
@EnableScheduling
public class AnalyseService {

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
        // Læser indstillingerne, fx datasættets placering og valg af model.
        PipelineConfig config =
                PipelineConfig.load(Path.of("config.properties"));

        // Bruges til at læse og skrive JSON i kommunikationen med modellen.
        ObjectMapper mapper = new ObjectMapper();
        // Opretter modelklienten via ProviderFactory, indtil A2 leverer Spring-plug-ins.
        ChatClient chat = ProviderFactory.chatClient(config, mapper);

        // Indlæser viderestillede samtaler fra det valgte datasæt og split.
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

        // TODO A4/A5: Kald Anonymizer og Validering her, når deres metoder er klar,
        // før transskriptionerne sendes til modeller eller skrives i cache.

        // Genbruger tidligere modelsvar fra disk, som i PipelineCli, så gentagne kørsler er hurtigere.
        LlmReasonExtractor llm = new LlmReasonExtractor(chat, mapper);
        ReasonExtractor extractor = new CachingReasonExtractor(
                llm,
                new JsonCache(config.cacheDir().resolve("reasons.json"), mapper),
                llm.cacheKeyPart(),
                mapper
        );

        // Samme indeks i listerne hører til samme samtale: årsagen bruges til embedding,
        // mens forklaringen senere skal vises sammen med resultatet.
        List<String> aarsager = new ArrayList<>();
        List<String> forklaringer = new ArrayList<>();

        // Udtrækker årsag og forklaring for hver samtale i datasættets rækkefølge.
        for (int i = 0; i < samtaler.size(); i++) {
            Conversation samtale = samtaler.get(i);
            TransferReason aarsag = extractor.extractReason(samtale.transcript());
            // TODO A5: Validér årsagsudtrækket og optæl/spring fejlede samtaler over.
            // Samtaler, årsager og forklaringer skal fortsat have samme indeks.
            aarsager.add(ReasonCleaner.clean(aarsag.primary()));
            forklaringer.add(aarsag.explanation());

            // Viser fremdriften, så længere modelkørsler kan følges i loggen.
            log.info("[{}/{}] Samtale {} – årsag: {}",
                    i + 1, samtaler.size(), samtale.id(), aarsager.getLast());
            log.info("Forklaring: {}", aarsag.explanation());
        }

        log.info("Årsagsudtræk færdigt for {} samtaler", aarsager.size());

        // Vælger embedding-modellen fra konfigurationen og genbruger tidligere vektorer fra disk.
        // Modelnavnet indgår i cache-nøglen, så forskellige modellers vektorer holdes adskilt.
        Embedder embedder = new CachingEmbedder(
                ProviderFactory.embedder(config, mapper),
                new JsonCache(config.cacheDir().resolve("embeddings.json"), mapper),
                ProviderFactory.embeddingCacheName(config),
                mapper
        );

        // Omdanner de korte årsager til talvektorer i samme rækkefølge som samtalerne.
        log.info("Laver embeddings med {}", config.activeEmbedding());
        double[][] vektorer = embedder.embed(aarsager).toArray(new double[0][]);
        // TODO A5: Validér embedding-output før normalisering og dimensionsreduktion.

        // Skalerer vektorerne til længde 1, så den efterfølgende afstandsberegning
        // sammenligner deres retning frem for deres længde, ligesom i PipelineCli.
        vektorer = VectorUtils.l2Normalize(vektorer);

        log.info("Embedding færdigt: {} vektorer med {} dimensioner",
                vektorer.length, vektorer[0].length);

        // Vælger UMAP fra konfigurationen. NoReduction bevarer vektorerne, hvis UMAP er slået fra.
        DimensionReducer reducer = config.umapEnabled()
                ? new UmapReducer(config.umapDimensions(), config.umapNeighbors())
                : new NoReduction();

        // Reducerer antallet af dimensioner før clustering og bevarer samtalernes rækkefølge.
        log.info("Dimensionsreduktion: {}", reducer.description());
        double[][] punkter;
        try {
            punkter = reducer.reduce(vektorer);
        } catch (RuntimeException | LinkageError e) {
            // Fortsætter med de oprindelige vektorer, hvis UMAP fejler, fx ved manglende native biblioteker.
            log.warn("Dimensionsreduktion fejlede; fortsætter uden reduktion", e);
            reducer = new NoReduction();
            punkter = reducer.reduce(vektorer);
        }

        log.info("Dimensionsreduktion færdig: {} punkter med {} dimensioner ({})",
                punkter.length, punkter[0].length, reducer.description());
        // TODO A5: Validér punkterne før clustering.

        // HDBSCAN grupperer lignende årsager. Hvert punkt får et cluster-id; -1 betyder støj.
        Clusterer clusterer = new HdbscanClusterer(config.minPoints(), config.minClusterSize());
        log.info("Clustering: {}", clusterer.description());
        int[] clusterIds = clusterer.cluster(punkter);
        // TODO A5: Kontrollér, at der er ét cluster-id pr. samtale.

        // Samler data midlertidigt, så vi kan finde eksempler fra hver gruppe til navngivning.
        Map<Integer, ClusterName> navne = new TreeMap<>();
        PipelineResult udenNavne = new PipelineResult(
                samtaler, aarsager, forklaringer, clusterIds, navne);

        // Modellen navngiver de grupper, HDBSCAN allerede har fundet, hvis navngivning er aktiveret.
        // Støj får intet gruppenavn, da disse samtaler ikke tilhører en egentlig gruppe.
        if (config.namingEnabled()) {
            ClusterNamer namer = new LlmClusterNamer(chat, mapper);
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

        // TODO A4/A5: Anonymisér og validér resultatet før gemning.
        // TODO A3: Send resultatet til Neo4jResultWriter, når dens gemmemetode er klar.
        // TODO A2: Erstat ProviderFactory og manuel oprettelse med plug-ins fra PluginConfig.
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
