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
    // W2: clusters med antal samtaler for én virksomhed i en periode, med søgning på navnet.
    // Samme filter-mønster som Idas SamtaleStatistikImpl, så tallene passer med overblikket.
    private static final String CLUSTERS_MED_ANTAL = """
            MATCH (s:Samtale)-[:TILHOERER]->(c:Cluster)
            WHERE s.tidspunkt >= $fra AND s.tidspunkt < $til
              AND (
                $companyId = ''
                OR EXISTS {
                  MATCH (:Virksomhed {id: $companyId})-[:HAR_KUNDE]->(:Kunde)-[:HAR]->(s)
                }
                OR EXISTS {
                  MATCH (:Virksomhed {id: $companyId})-[:HAR_AI_AGENT]->(:AiAgent)-[:HAANDTERER]->(s)
                }
              )
              AND ($search = '' OR toLower(coalesce(c.navn, '')) CONTAINS toLower($search))
            RETURN c.id AS id, c.navn AS navn, c.beskrivelse AS beskrivelse, count(DISTINCT s) AS antal
            ORDER BY antal DESC, navn ASC
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


    @Override // Vi udfylder den nye metode fra interfacet ClusterStatistik
    public List<ClusterTal> hentClustersMedAntal(String companyId, LocalDateTime fra, LocalDateTime til, String search) {
        var resultat = neo4jClient
                .query(CLUSTERS_MED_ANTAL)          // Den nye Cypher
                .bind(companyId).to("companyId")    // Sæt $companyId ("" = alle virksomheder)
                .bind(fra).to("fra")                // Sæt $fra
                .bind(til).to("til")                // Sæt $til
                .bind(search).to("search")          // Sæt $search ("" = ingen søgning)
                .fetchAs(ClusterTal.class)          // Hver række bliver til en ClusterTal
                .mappedBy((typeSystem, raekke) -> new ClusterTal(
                        raekke.get("id").asString(),
                        raekke.get("navn").asString(""),        // "" hvis navnet mangler
                        raekke.get("beskrivelse").asString(""), // "" hvis beskrivelsen mangler
                        raekke.get("antal").asLong()))
                .all();                             // Kør og hent alle rækker
        return new ArrayList<>(resultat);           // Returnér som almindelig liste
    }
}