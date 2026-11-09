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

import com.cendoAiAgentAnalysis.backend.analyse.config.ProviderFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

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
                    .map(text -> new double[]{1.0, 2.0, 3.0}) //Omdanner hver tekst om til en kunstig vektor - her med 3 dimensioner
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

    /* TEST AF RIGTIG PIPELINE KAN BRUGE FALSK PLUGIN / FAKE EMBEDDER - ALTSÅ UDSKIFTELIGHED */

    @Test
    void rigtigPipelineKanBrugeFakeEmbedder() throws Exception {
        Properties properties = new Properties();  // Indlæser projektets konfiguration af indstillinger

        try (var reader = java.nio.file.Files.newBufferedReader(
                Path.of("config.properties"))) { // når vi som i test lige må oprette objekterne selv, læser vi dem fra config.properties
            properties.load(reader);
        }

        // Undgå at kalde Ollama ved at ændre i nogle af indstillingerne som ellers læses.

        properties.setProperty("pipeline.mode", "FULL_TRANSCRIPT"); //bruger samtaleteksten i stedet for først at bede AI om at udtrække årsagen.
        properties.setProperty("naming.enabled", "false"); //slår navngivning af grupper fra
        properties.setProperty("dataset.split", "train"); //bruger træningsdelen af excel datasaættet

        PipelineConfig config = PipelineConfig.fromProperties(properties); //opretter konfigurationen ud fra indstillinger


        Embedder fakeEmbedder = new FakeEmbedder(); // Bruger vores FakeEmbedder i den rigtige pipeline

        DimensionReducer fakeReducer = new DimensionReducer() {
            @Override
            public double[][] reduce(double[][] vectors) {
                return vectors;
            }

            @Override
            public String description() { //beskriver hvilken plugin implementering vi bruger
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
            public String description() { //beskriver hvilken plugin implementering vi bruger
                return "Fake clusterer";
            }
        };

        AnalyseService pipeline = new AnalyseService( //Vi tester pipelinen og ikke AI-modellerne
                config,                                     //indstillingerne
                text -> new TransferReason(text, ""),       //Falsk årsagsudtrækker - bruger teksten direkte som årsag og en tom forklaring
                fakeEmbedder,                               //omdanner tekst til kunstige talvektorer
                fakeReducer,                                //Returnerer vektoreren uændret
                fakeClusterer,                              //Placerer all punkter i samme gruppe
                examples -> null                            //Falsk clusternavn og returnerer intet navn.
        );

        // Kører den rigtige analyseservice med falske plugins og gemmer resultatet
        var resultat = pipeline.koerAnalyseMedDetaljer();   // var - java finder selv ud af variablens type som bliver returneret.

        // Kontrollérer resultatet med JUnit
        assertTrue(resultat.inputCount() > 0);              //kontrollerer at der bliver indlæst samtaler
        assertEquals(0, resultat.conversationErrors());     //ingen samtaler fejler
        assertEquals(resultat.inputCount(), resultat.embeddings().length);      //intallet af embeddingvektorer er samme som antal samtaler
        assertEquals(3, resultat.embeddings()[0].length);   //dimensioner (som forventes at være 3 - ud fra vores fakeEmbedder)
    }

    /* TEST AF RIGTIG PIPELINE KAN BRUGE FALSK PLUGIN / FAKE EMBEDDER - ALTSÅ UDSKIFTELIGHED */



    /* TEST AF RIGTIG EMBEDDER - RETURNERER 768 DIMENSIONER? */

    @Test
    @EnabledIfEnvironmentVariable(          // køres naturligvis kun hvis miljøvariablen er enabed
            named = "RUN_OLLAMA_TESTS",
            matches = "true"
    )
    void rigtigEmbedderReturnerer768Dimensioner() throws Exception {

        // Henter konfiguration - indstillingerne
        PipelineConfig config = PipelineConfig.load(
                Path.of("config.properties")    //når vi selv oprettet objekter, henter vi dem herfra frem for Spring
        );


        Embedder embedder = ProviderFactory.embedder(   // Opretter den rigtige embedder via ProviderFactory, som konfigurationen angiver - her OllamaEmbedder
                config,
                new ObjectMapper()                      // til håndtering af JSON, når vi kommunikerer med Ollama
        );


        List<double[]> result = embedder.embed(         //sender én tekst til ollamamodellen og får en embedding tilbage
                List.of("Hej, jeg vil gerne viderestilles")
        );

        // Kontrollér resultatet med JUnit - hvis begge passer, består testen selvfølgelig.
        assertEquals(1, result.size());                  //Én tekst -> én vektor
        assertEquals(768, result.get(0).length);         //første vektor skal indeholde 768 tal
    }

    /* TEST AF RIGTIG EMBEDDER - RETURNERER 768 DIMENSIONER? */

}