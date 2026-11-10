
package com.cendoAiAgentAnalysis.backend.analyse.output;

import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;
import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;
import com.cendoAiAgentAnalysis.backend.analyse.NyeAarsager;
import com.cendoAiAgentAnalysis.backend.analyse.util.VectorUtils;
import com.cendoAiAgentAnalysis.backend.dataadgang.ClusterRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import com.cendoAiAgentAnalysis.backend.model.Cluster;
import com.cendoAiAgentAnalysis.backend.model.Samtale;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;


/**
 * Neo4jResultWriter – gemmer analyseresultatet i Neo4j.
 *
 * 1. Tjekker at data er i orden.
 * 2. finder clusters, der ikke fandtes sidste gang (via NyeAarsager).
 * 3. Gemmer hver samtale, og forbinder den til sit cluster.
 *    gemmer også samtalens vektor, så vi kan finde lignende samtaler.
 *    giver hvert cluster sit midtpunkt og markerer det som nyt (erNy).
 *
 * write(result) uden vektorer virker som før – så springes A6 og V1 over.
 */


@Component
public class Neo4jResultWriter {

    // Repositories bruges til at gemme clusters og samtaler i Neo4j
    private final ClusterRepository clusterRepository;
    private final SamtaleRepository samtaleRepository;

    // Modtager de to repositories gennem constructor injection
    public Neo4jResultWriter(
            ClusterRepository clusterRepository,
            SamtaleRepository samtaleRepository) {

        this.clusterRepository = clusterRepository;
        this.samtaleRepository = samtaleRepository;
    }

    // Gemmer analyseresultatet i Neo4j
    // Transactional sikrer, at ændringer rulles tilbage ved fejl
    @Transactional
    public void write(PipelineResult result) {
        write(result, null); // Gammel måde: uden vektorer (tests bruger stadig denne)
    }

    // A6 + V1: samme som write(result), men gemmer også embedding på Samtale
    // og centroide + erNy på Cluster. embeddings[i] hører til samtale i.
    @Transactional
    public void write(PipelineResult result, double[][] embeddings) {

        // Kontrollerer at analyseresultatet ikke er null
        Objects.requireNonNull(
                result,
                "PipelineResult må ikke være null"
        );

        int conversationCount = result.conversations().size();

        // Kontrollerer at alle samtaler har analyseresultater
        if (conversationCount != result.clusterIds().length
                || conversationCount != result.texts().size()
                || conversationCount != result.explanations().size()) {

            throw new IllegalArgumentException(
                    "Antallet af samtaler og analyseresultater stemmer ikke overens"
            );
        }

        // Kontrollerer at alle almindelige clusters har et navn
        // Cluster -1 er støj og kræver derfor ikke et navn
        for (int clusterId : result.clusterIds()) {

            if (clusterId == -1) {
                continue;
            }

            if (result.names() == null
                    || result.names().get(clusterId) == null) {

                throw new IllegalArgumentException(
                        "Mangler oplysninger om cluster " + clusterId
                );
            }
        }

        // Holder styr på clusters, så hvert cluster kun oprettes
        // én gang under den aktuelle gennemløbning
        Map<Integer, Cluster> savedClusters = new HashMap<>();

        // A6: find centroider for denne kørsel og sammenlign med sidste kørsel
        Map<Integer, double[]> nyeCentroider = new HashMap<>();
        Set<Integer> nyeIds = Set.of();
        if (embeddings != null) {
            if (embeddings.length != conversationCount) {
                throw new IllegalArgumentException("Antal embeddings passer ikke med antal samtaler");
            }
            NyeAarsager nyeAarsager = new NyeAarsager();
            nyeCentroider = nyeAarsager.centroider(embeddings, result.clusterIds());

            // Hent gamle centroider FØR vi gemmer nye clusters
            List<double[]> gamle = new ArrayList<>();
            for (Cluster gammel : clusterRepository.findAll()) {
                if (gammel.getCentroide() != null && !gammel.getCentroide().isEmpty()) {
                    gamle.add(VectorUtils.tilArray(gammel.getCentroide()));
                }
            }
            nyeIds = nyeAarsager.findNye(nyeCentroider, gamle);
        }

        // Gennemgår alle samtaler fra pipelinen
        for (int i = 0; i < conversationCount; i++) {

            // Henter den aktuelle samtale
            var conversation = result.conversations().get(i);

            // Henter samtalens cluster-ID
            int clusterId = result.clusterIds()[i];

            // Henter den korte viderestillingsårsag
            String reason = result.texts().get(i);

            // Henter den længere forklaring
            String explanation = result.explanations().get(i);

            // Finder en eksisterende samtale i databasen
            // Hvis samtalen ikke findes, oprettes en ny
            Samtale samtale = samtaleRepository
                    .findById(conversation.id())
                    .orElseGet(() -> new Samtale(
                            conversation.id(),
                            reason,
                            explanation,
                            null
                    ));

            // Opdaterer samtalens tekst og analyseresultat
            samtale.setTranscript(conversation.transcript());
            samtale.setViderestillingsaarsag(reason);
            samtale.setForklaring(explanation);

            // V1: gem vektoren, så Neo4j kan finde lignende samtaler
            if (embeddings != null) {
                samtale.setEmbedding(VectorUtils.tilListe(embeddings[i]));
            }

            // Fjerner den tidligere clusterreference i objektet
            // Den nye relation gemmes sammen med samtalen
            samtale.setCluster(null);

            if (clusterId == -1) {

                // Støj gemmes som en samtale uden cluster
                System.out.println(
                        "Samtale " + conversation.id() + " er støj"
                );

            } else {

                // Undersøger om clusteret allerede er oprettet
                // under denne gennemløbning
                Cluster cluster = savedClusters.get(clusterId);

                if (cluster == null) {

                    // Henter clusterets navn og beskrivelse
                    ClusterName clusterName =
                            result.names().get(clusterId);

                    String name = clusterName.name();
                    String description = clusterName.description();

                    // Finder antallet af samtaler i clusteret
                    int size = result.membersByCluster()
                            .get(clusterId)
                            .size();

                    // Opretter clusteret med navn, beskrivelse
                    // og antal tilknyttede samtaler
                    cluster = new Cluster(
                            name,
                            description,
                            size
                    );

                    // gem centroiden og om årsagen er ny
                    double[] centroide = nyeCentroider.get(clusterId);
                    if (centroide != null) {
                        cluster.setCentroide(VectorUtils.tilListe(centroide));
                    }
                    cluster.setErNy(nyeIds.contains(clusterId));

                    // Gemmer clusteret i Neo4j
                    cluster = clusterRepository.save(cluster);

                    // Gemmer clusteret i vores map
                    // Så det ikke oprettes flere gange i samme kørsel
                    savedClusters.put(clusterId, cluster);
                }

                // Forbinder samtalen med det korrekte cluster
                samtale.setCluster(cluster);

                System.out.println(
                        "Samtale " + conversation.id()
                                + " tilhører cluster " + clusterId
                );
            }

            // Gemmer eller opdaterer samtalen i Neo4j
            // Samtalens clusterrelation gemmes samtidig
            samtaleRepository.save(samtale);
        }
    }
}
