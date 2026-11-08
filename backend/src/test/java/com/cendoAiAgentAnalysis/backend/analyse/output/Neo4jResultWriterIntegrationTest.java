package com.cendoAiAgentAnalysis.backend.analyse.output;

import com.cendoAiAgentAnalysis.backend.analyse.model.Conversation;
import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;
import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;
import com.cendoAiAgentAnalysis.backend.dataadgang.ClusterRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import com.cendoAiAgentAnalysis.backend.model.Cluster;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class Neo4jResultWriterIntegrationTest {

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private SamtaleRepository samtaleRepository;

    private Neo4jResultWriter writer;

    // Gemmer ID'er på de clusters, som den aktuelle test opretter
    private final List<String> createdClusterIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Opretter writeren med de rigtige Neo4j-repositories
        writer = new Neo4jResultWriter(
                clusterRepository,
                samtaleRepository
        );

        createdClusterIds.clear();
    }

    @AfterEach
    void cleanUp() {

        // Sletter kun de testsamtaler, som denne test har oprettet
        for (String id : List.of(
                "TEST-NEO4J-001",
                "TEST-MULTI-001",
                "TEST-MULTI-002",
                "TEST-NOISE-001"
        )) {
            if (samtaleRepository.existsById(id)) {
                samtaleRepository.deleteById(id);
            }
        }

        // Sletter bagefter de clusters, som testen selv har oprettet
        for (String clusterId : createdClusterIds) {
            if (clusterRepository.existsById(clusterId)) {
                clusterRepository.deleteById(clusterId);
            }
        }
    }

    @Test
    void shouldConnectToNeo4j() {

        // Kontrollerer at Spring Boot kan hente data fra Neo4j
        long antalSamtaler = samtaleRepository.count();
        long antalClusters = clusterRepository.count();

        System.out.println("Antal samtaler: " + antalSamtaler);
        System.out.println("Antal clusters: " + antalClusters);
    }

    @Test
    void shouldSavePipelineResultToNeo4j() {

        // Opretter en fiktiv kundesamtale
        Conversation conversation = new Conversation(
                "TEST-NEO4J-001",
                "test",
                "Jeg vil gerne tale med en medarbejder om min faktura"
        );

        // Opretter analyseresultatet
        PipelineResult result = new PipelineResult(
                List.of(conversation),
                List.of("Spørgsmål om faktura"),
                List.of("Kunden ønsker hjælp til sin faktura"),
                new int[]{0},
                Map.of(
                        0,
                        new ClusterName(
                                "Fakturaproblemer",
                                "Samtaler om fakturaer"
                        )
                )
        );

        // Gemmer analyseresultatet i Neo4j
        writer.write(result);

        // Kontrollerer at samtalen er blevet gemt
        var savedSamtale = samtaleRepository.findById("TEST-NEO4J-001");

        assertTrue(savedSamtale.isPresent());

        // Gemmer ID'et på det oprettede cluster til oprydning
        Cluster cluster = savedSamtale.get().getCluster();
        assertNotNull(cluster);
        createdClusterIds.add(cluster.getId());
    }

    @Test
    void shouldSaveMultipleConversationsAndNoise() {

        // Opretter tre fiktive kundesamtaler
        Conversation conversation1 = new Conversation(
                "TEST-MULTI-001",
                "test",
                "Jeg har et spørgsmål om min faktura"
        );

        Conversation conversation2 = new Conversation(
                "TEST-MULTI-002",
                "test",
                "Jeg er blevet opkrævet for meget"
        );

        Conversation conversation3 = new Conversation(
                "TEST-NOISE-001",
                "test",
                "Hej, jeg ville bare sige tak"
        );

        // De første to samtaler tilhører samme cluster
        // Den tredje samtale er støj (-1)
        PipelineResult result = new PipelineResult(
                List.of(conversation1, conversation2, conversation3),
                List.of(
                        "Spørgsmål om faktura",
                        "Forkert opkrævning",
                        "Ingen viderestillingsårsag"
                ),
                List.of(
                        "Kunden ønsker hjælp til sin faktura",
                        "Kunden mener, at beløbet er forkert",
                        "Samtalen indeholder ingen relevant årsag"
                ),
                new int[]{0, 0, -1},
                Map.of(
                        0,
                        new ClusterName(
                                "Fakturaproblemer",
                                "Samtaler om fakturaer"
                        )
                )
        );

        // Gemmer analyseresultatet i Neo4j
        writer.write(result);

        // Henter de gemte samtaler
        var saved1 = samtaleRepository.findById("TEST-MULTI-001");
        var saved2 = samtaleRepository.findById("TEST-MULTI-002");
        var saved3 = samtaleRepository.findById("TEST-NOISE-001");

        // Kontrollerer at alle tre samtaler er gemt
        assertTrue(saved1.isPresent());
        assertTrue(saved2.isPresent());
        assertTrue(saved3.isPresent());

        // Kontrollerer at de første to samtaler har et cluster
        assertNotNull(saved1.get().getCluster());
        assertNotNull(saved2.get().getCluster());

        // Kontrollerer at de tilhører samme cluster
        assertEquals(
                saved1.get().getCluster().getId(),
                saved2.get().getCluster().getId()
        );

        // Kontrollerer at støjsamtalen ikke har et cluster
        assertNull(saved3.get().getCluster());

        // Gemmer cluster-ID'et, så det kan slettes efter testen
        createdClusterIds.add(saved1.get().getCluster().getId());
    }
}