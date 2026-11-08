package com.cendoAiAgentAnalysis.backend.analyse.config;

import com.cendoAiAgentAnalysis.backend.dataadgang.AiAgentRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.ClusterRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.KundeRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.VirksomhedRepository;
import com.cendoAiAgentAnalysis.backend.model.AiAgent;
import com.cendoAiAgentAnalysis.backend.model.Cluster;
import com.cendoAiAgentAnalysis.backend.model.Kunde;
import com.cendoAiAgentAnalysis.backend.model.Samtale;
import com.cendoAiAgentAnalysis.backend.model.Virksomhed;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.boot.CommandLineRunner; //Allows the class to run code at application startup, so "Run" is called
import org.springframework.stereotype.Component; //Does so springboot can find this class and run it at startup

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class TestData implements CommandLineRunner {

    private final KundeRepository kundeRepository;
    private final AiAgentRepository aiAgentRepository;
    private final VirksomhedRepository virksomhedRepository;
    private final ClusterRepository clusterRepository;
    private final SamtaleRepository samtaleRepository;
    private final Neo4jClient neo4jClient;

    // Constructor  - injection of the repositories, so we can use them to save and retrieve data from the database
    public TestData(
            KundeRepository kundeRepository,
            AiAgentRepository aiAgentRepository,
            VirksomhedRepository virksomhedRepository,
            ClusterRepository clusterRepository,
            SamtaleRepository samtaleRepository,
            Neo4jClient neo4jClient) {
        this.kundeRepository = kundeRepository;
        this.aiAgentRepository = aiAgentRepository;
        this.virksomhedRepository = virksomhedRepository;
        this.clusterRepository = clusterRepository;
        this.samtaleRepository = samtaleRepository;
        this.neo4jClient = neo4jClient;
    }

    @Override
    public void run(String... args) {
        rydDemoVirksomhedsrelationer();

        // Create and save test customers
        Kunde kunde1 = new Kunde("1", "Kunde 1: Anna");
        Kunde kunde2 = new Kunde("2", "Kunde 2: Bob");

        // Calls the save method of the kundeRepository to persist the Kunde objects in the database
        kundeRepository.save(kunde1);
        kundeRepository.save(kunde2);

        // Create and save test AI agents
        AiAgent aiAgent1 = new AiAgent("1", "AI Agent 1");
        AiAgent aiAgent2 = new AiAgent("2", "AI Agent 2");

        //Calls the save method of the aiAgentRepository to persist the AiAgent objects in the database
        aiAgentRepository.save(aiAgent1);
        aiAgentRepository.save(aiAgent2);

        // Demo-samtalerne tilhører kun Værksted Nord via kunde 1 og AI-agent 1.
        Virksomhed virksomhed1 = virksomhedRepository.findById("virksomhed-1")
                .orElseGet(() -> new Virksomhed("virksomhed-1", "Værksted Nord"));
        virksomhed1.setKunder(List.of(kunde1));
        virksomhed1.setAiAgenter(List.of(aiAgent1));

        // Bilhuset Syd har sine egne relationer, men ingen seedede samtaler.
        Virksomhed virksomhed2 = virksomhedRepository.findById("virksomhed-2")
                .orElseGet(() -> new Virksomhed("virksomhed-2", "Bilhuset Syd"));
        virksomhed2.setKunder(List.of(kunde2));
        virksomhed2.setAiAgenter(List.of(aiAgent2));

        virksomhedRepository.save(virksomhed1);
        virksomhedRepository.save(virksomhed2);

        seedOverblikTestData(kunde1, aiAgent1);

        // Prints the saved customers to the console
        System.out.println("\nKunder");
        kundeRepository.findAll().forEach(kunde ->
                System.out.println(
                        "ID: " + kunde.getId()
                        + ", Navn: " + kunde.getNavn()
                )
        );

        // Prints the saved AI agents to the console
        System.out.println("\nAI-agenter");
        aiAgentRepository.findAll().forEach(agent ->
                System.out.println(
                        "ID: " + agent.getId()
                        + ", Navn: " + agent.getNavn()
                )
        );

        System.out.println("\nVirksomheder");
        virksomhedRepository.findAll().forEach(virksomhed ->
                System.out.println(
                        "ID: " + virksomhed.getId()
                        + ", Navn: " + virksomhed.getNavn()
                )
        );

        // Prints the count of customers, AI agents and companies in the database to the console
        System.out.println("\nAntal kunder: " + kundeRepository.count());
        System.out.println("Antal AI-agenter: " + aiAgentRepository.count());
        System.out.println("Antal virksomheder: " + virksomhedRepository.count());
    }

    private void rydDemoVirksomhedsrelationer() {
        neo4jClient.query("""
                MATCH (v:Virksomhed)
                WHERE v.id IN ['virksomhed-1', 'virksomhed-2']
                OPTIONAL MATCH (v)-[r:HAR_KUNDE|HAR_AI_AGENT]->()
                WITH r
                WHERE r IS NOT NULL
                DELETE r
                """).run();
    }

    // Test data for overblikssiden: clusters og samtaler til top 5 årsager og tidslinje. Samtalerne fordeles over de sidste 25 dage, så der er noget at vise i tidslinjen.
    private void seedOverblikTestData(Kunde kunde, AiAgent aiAgent) {
        List<String> clusterNames = List.of(
                "Demo: Status på reparation",
                "Demo: Garanti",
                "Demo: Faktura",
                "Demo: Ombooking",
                "Demo: Lånebil");
        int[] conversationCounts = {5, 4, 3, 2, 1};
        LocalDateTime now = LocalDateTime.now();
        List<Cluster> existingClusters = new ArrayList<>(clusterRepository.findAll());

        for (int clusterIndex = 0; clusterIndex < clusterNames.size(); clusterIndex++) {
            String clusterName = clusterNames.get(clusterIndex);
            int conversationCount = conversationCounts[clusterIndex];
            Cluster cluster = existingClusters.stream()
                    .filter(existing -> clusterName.equals(existing.getNavn()))
                    .findFirst()
                    .orElse(null);
            if (cluster == null) {
                cluster = clusterRepository.save(new Cluster(
                        clusterName,
                        "Demo-data til afprøvning af overblikket",
                        conversationCount));
                existingClusters.add(cluster);
            }

            for (int conversationIndex = 0; conversationIndex < conversationCount; conversationIndex++) {
                String conversationId = "demo-overblik-" + (clusterIndex + 1) + "-" + (conversationIndex + 1);
                int daysAgo = (clusterIndex * 5 + conversationIndex) % 25;
                LocalDateTime tidspunkt = now.minusDays(daysAgo).withHour(9 + conversationIndex);
                Samtale samtale = new Samtale(
                        conversationId,
                        clusterName,
                        "Demo-samtale til test af overblikket",
                        tidspunkt);
                samtale.setCluster(cluster);
                samtale.setKunde(kunde);
                samtale.setAiAgent(aiAgent);
                // Gem også eksisterende demo-samtaler igen, så manglende relationer bliver repareret.
                samtaleRepository.save(samtale);
            }
        }
    }
}