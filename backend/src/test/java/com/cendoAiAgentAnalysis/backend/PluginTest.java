// ID:          E1
// Hvad:        Test hvert plug-in alene (ReasonExtractor, Embedder, DimensionReducer, Clusterer, ClusterNamer) og at et plug-in kan udskiftes uden ændringer andre steder (designmål 2).
package com.cendoAiAgentAnalysis.backend;

import com.cendoAiAgentAnalysis.backend.analyse.AnalyseService;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import com.cendoAiAgentAnalysis.backend.analyse.model.TransferReason;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.DimensionReducer;

import java.nio.file.Path;
import java.util.Properties;
import java.util.Arrays;


import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.clustering.HdbscanClusterer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class PluginTest {

    /* TEST AF EMBEDDING-PLUGIN */
    static class FakeEmbedder implements Embedder { //laver fake embedder

        @Override //implementerer interfacets metode
        public List<double[]> embed(List<String> texts) { //laver fake metode til testen - en ollama-embedder har sin egen implementering. Alle bruger samme interface, så resten af systemet kan bruge dem på samme måde.
            return texts.stream()//gennemgår teksterne én ad gangen -metode fra java
                    .map(text -> new double[]{1.0, 2.0, 3.0}) //Omdanner hver tekst om til en kunstig vektor
                    .toList(); //samler resultaterne i en ny liste.
        }

    }
    @Test
        //selve testen som tester at vores fake-plugin returnerer to vektorer med 3 tal i hver
    void fakeEmbedderReturnererEnVektorPerTekst() throws Exception {
        Embedder embedder = new FakeEmbedder(); //opretter som objekt

        List<double[]> result = embedder.embed( //kalder interfacets method
                List.of("Hej", "Farvel")
        );

        assertEquals(2, result.size()); //assertEquals er method fra JUnit, som kontrollerer om to værdier er ens. Regner med to + og hvad vi rent faktisk får. (vi regner med 2 vektorer)
        assertEquals(3, result.get(0).length); //henter første vektor. Length beskriver antal dimensioner - så her regner vi med 3 dimensioner
    }

    @Test
    void embedderKanUdskiftes() throws Exception {
        Embedder embedder1 = new FakeEmbedder(); //bruger vores fakeEmbedder

        Embedder embedder2 = texts -> texts.stream() //bruger en anden implementering af Embedder, skrevet som lambda (kort måde at skrive funktion) - men begge funktioner kan kaldes med .embed()
                .map(text -> new double[]{4.0, 5.0, 6.0})
                .toList();

        assertEquals(2, embedder1.embed(List.of("Hej", "Farvel")).size()); //tester begge embeddere
        assertEquals(2, embedder2.embed(List.of("Hej", "Farvel")).size());
    }

    /* TEST AF EMBEDDING-PLUGIN */

    /* TEST AF CLUSTER-PLUGIN */

    @Test
    void clustererFinderToGrupper() { //opretter rigtig HdbscanClusterer - tester om kan finde to tydelige punkter
        Clusterer clusterer = new HdbscanClusterer(2, 3);

        double[][] points = {         //giver den 6 punkter fordelt på 2 grupper
                {0.0, 0.0}, {0.1, 0.0}, {0.0, 0.1},
                {10.0, 10.0}, {10.1, 10.0}, {10.0, 10.1}
        };

        int[] labels = clusterer.cluster(points); //cluster() returnerer et gruppenummer for hvert punkt

        assertEquals(6, labels.length); //kontrollere at punkter grupperes som forventet.
        assertNotEquals(Clusterer.NOISE, labels[0]); //kontrollere at ingen bliver markeret som støj
        assertNotEquals(Clusterer.NOISE, labels[3]);
        assertEquals(labels[0], labels[1]);
        assertEquals(labels[0], labels[2]);
        assertEquals(labels[3], labels[4]);
        assertEquals(labels[3], labels[5]);
        assertNotEquals(labels[0], labels[3]);
    }

    /* TEST AF CLUSTER-PLUGIN */


    @Test
    void rigtigPipelineKanBrugeFakeEmbedder() throws Exception {
        // 1. Indlæs projektets konfiguration
        Properties properties = new Properties();

        try (var reader = java.nio.file.Files.newBufferedReader(
                Path.of("config.properties"))) {
            properties.load(reader);
        }

        // 2. Undgå at kalde Ollama
        properties.setProperty("pipeline.mode", "FULL_TRANSCRIPT");
        properties.setProperty("naming.enabled", "false");
        properties.setProperty("dataset.split", "train");

        PipelineConfig config = PipelineConfig.fromProperties(properties);

        // 3. Brug vores FakeEmbedder i den rigtige pipeline
        Embedder fakeEmbedder = new FakeEmbedder();

        DimensionReducer fakeReducer = new DimensionReducer() {
            @Override
            public double[][] reduce(double[][] vectors) {
                return vectors;
            }

            @Override
            public String description() {
                return "Fake reducer";
            }
        };

        Clusterer fakeClusterer = new Clusterer() {
            @Override
            public int[] cluster(double[][] points) {
                int[] labels = new int[points.length];
                Arrays.fill(labels, 0);
                return labels;
            }

            @Override
            public String description() {
                return "Fake clusterer";
            }
        };

        AnalyseService pipeline = new AnalyseService(
                config,
                text -> new TransferReason(text, ""),
                fakeEmbedder,
                fakeReducer,
                fakeClusterer,
                examples -> null
        );

        // 4. Kør den rigtige analyse
        var resultat = pipeline.koerAnalyseMedDetaljer();

        // 5. Kontrollér resultatet med JUnit
        assertTrue(resultat.inputCount() > 0);
        assertEquals(0, resultat.conversationErrors());
        assertEquals(resultat.inputCount(), resultat.embeddings().length);
        assertEquals(3, resultat.embeddings()[0].length);
    }

}