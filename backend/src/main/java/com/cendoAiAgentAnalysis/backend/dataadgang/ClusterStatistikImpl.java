package com.cendoAiAgentAnalysis.backend.dataadgang;

import org.springframework.data.neo4j.core.Neo4jClient; // Springs værktøj til at køre Cypher direkte

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Koden bag ClusterStatistik.
// Navnet SKAL være interfacets navn + "Impl". Sådan finder Spring den og kobler den på ClusterRepository.
public class ClusterStatistikImpl implements ClusterStatistik {

    // Cypher-forespørgslen. static final = en konstant, der aldrig ændres
    private static final String ANTAL_PR_CLUSTER = """
            MATCH (k:Kunde {id: $kundeId})-[:HAR]->(s:Samtale)-[:TILHOERER]->(c:Cluster)
            WHERE s.tidspunkt >= $fra AND s.tidspunkt < $til
            RETURN c.id AS clusterId, c.navn AS clusterNavn, count(s) AS antal
            ORDER BY antal DESC
            """;

    private final Neo4jClient neo4jClient; // Forbindelsen til Neo4j

    // Spring giver os selv en Neo4jClient (dependency injection)
    public ClusterStatistikImpl(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @Override // Vi udfylder metoden fra interfacet
    public List<ClusterAntal> antalSamtalerPrCluster(String kundeId, LocalDateTime fra, LocalDateTime til) {
        var resultat = neo4jClient
                .query(ANTAL_PR_CLUSTER)        // Den Cypher, der skal køres
                .bind(kundeId).to("kundeId")    // Sæt $kundeId
                .bind(fra).to("fra")            // Sæt $fra
                .bind(til).to("til")            // Sæt $til
                .fetchAs(ClusterAntal.class)    // Hver række bliver til en ClusterAntal
                .mappedBy((typeSystem, raekke) -> new ClusterAntal(
                        raekke.get("clusterId").asString(),   // kolonnen clusterId -> tekst
                        raekke.get("clusterNavn").asString(), // kolonnen clusterNavn -> tekst
                        raekke.get("antal").asLong()))        // kolonnen antal -> tal
                .all();                         // Kør og hent alle rækker
        return new ArrayList<>(resultat);       // Returnér som almindelig liste
    }
}