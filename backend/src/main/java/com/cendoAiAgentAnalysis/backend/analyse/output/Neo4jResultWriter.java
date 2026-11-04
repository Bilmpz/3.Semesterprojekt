package com.cendoAiAgentAnalysis.backend.analyse.output;

import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;
import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;
import com.cendoAiAgentAnalysis.backend.analyse.util.VectorUtils;
import com.cendoAiAgentAnalysis.backend.model.Cluster;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Neo4jResultWriter {

    /** Uændret: virker præcis som før (bruges af eksisterende kode). */
    public void write(PipelineResult result) {
        write(result, Map.of(), Set.of());
    }

    /**
     * A6: samme som write(result), men laver også Cluster-objekterne
     * med centroide og erNy-markering.
     *
     * @param centroider cluster-id -> centroide (fra NyeAarsager.centroider)
     * @param nyeIds     cluster-id'er der er nye (fra NyeAarsager.findNye)
     * @return de færdige Cluster-objekter – klar til clusterRepository.saveAll(...) (A3)
     */
    public Collection<Cluster> write(PipelineResult result,
                                     Map<Integer, double[]> centroider,
                                     Set<Integer> nyeIds) {

        Objects.requireNonNull(result, "PipelineResult må ikke være null");
        Objects.requireNonNull(centroider, "centroider må ikke være null");
        Objects.requireNonNull(nyeIds, "nyeIds må ikke være null");

        int conversationCount = result.conversations().size();

        // Kontrollerer at alle samtaler har analyseresultater
        if (conversationCount != result.clusterIds().length
                || conversationCount != result.texts().size()
                || conversationCount != result.explanations().size()) {

            throw new IllegalArgumentException(
                    "Antallet af samtaler og analyseresultater stemmer ikke overens"
            );
        }

        // Ét Cluster-objekt pr. cluster-id
        Map<Integer, Cluster> clusters = new LinkedHashMap<>();

        for (int i = 0; i < conversationCount; i++) {

            var conversation = result.conversations().get(i);

            int clusterId = result.clusterIds()[i];
            // korte
            String reason = result.texts().get(i);
            // Lange forklaring
            String explanation = result.explanations().get(i);

            if (clusterId == -1) {
                // Samtalen er klassificeret som støj
                System.out.println("Samtale " + conversation.id() + " er støj");
            } else {
                // Samtalen tilhører et almindeligt cluster
                ClusterName clusterName = result.names().get(clusterId);
                if (clusterName == null) {
                    throw new IllegalArgumentException(
                            "Mangler oplysninger om cluster " + clusterId
                    );
                }
                String name = clusterName.name();
                String description = clusterName.description();

                // Lav clusteret første gang, vi ser cluster-id'et
                Cluster cluster = clusters.computeIfAbsent(clusterId, id -> {
                    Cluster c = new Cluster(name, description, 0);

                    // A6: centroide og "ny årsag" (kun hvis de er givet med)
                    double[] centroide = centroider.get(id);
                    if (centroide != null) {
                        c.setCentroide(VectorUtils.tilListe(centroide));
                    }
                    c.setErNy(nyeIds.contains(id));

                    return c;
                });
                cluster.setStoerrelse(cluster.getStoerrelse() + 1);

                System.out.println("Samtale " + conversation.id() + " tilhører cluster " + clusterId);
            }
        }

        // Udskriv nye årsager, så det kan ses i loggen
        for (var e : clusters.entrySet()) {
            if (e.getValue().isErNy()) {
                System.out.println("NY ÅRSAG: cluster " + e.getKey() + " – " + e.getValue().getNavn());
            }
        }

        return clusters.values();
    }
}