package com.cendoAiAgentAnalysis.backend.dataadgang;

import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Implementering af SamtaleStatistik, som definerer metoder til at hente statistikdata fra Neo4j-databasen. Denne klasse bruger Neo4jClient til at udføre Cypher-forespørgsler og returnere resultaterne som OverblikTal-objekter.
// Overordnet implementerer denne klasse statestikmetoden fra SamtaleStatestik, og indeholder cypher-forespørgslen til at hente overbliksdata for samtaler, årsager og udvikling baseret på virksomhed, tidsperiode og søgeord.
public class SamtaleStatistikImpl implements SamtaleStatistik {
    private static final String OVERBLIKSTAL = """
            MATCH (s:Samtale)
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
            OPTIONAL MATCH (s)-[:TILHOERER]->(c:Cluster)
            WITH s, c
            WHERE $search = ''
              OR (c IS NOT NULL AND toLower(coalesce(c.navn, '')) CONTAINS toLower($search))
            RETURN count(DISTINCT s) AS totalConversations, 
                   count(DISTINCT c) AS causesFound
            """;
    // Henter top 5 årsager (clusters) for samtaler i den angivne periode, filtreret efter virksomhed og søgeord. Resultaterne sorteres efter antal samtaler og cluster-navn.
    private static final String TOP_AARSAGER = """
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
            RETURN c.id AS id, c.navn AS title, count(DISTINCT s) AS count
            ORDER BY count DESC, title ASC
            LIMIT 5
            """;

    private static final String SAMTALER_PR_DAG = """
            MATCH (s:Samtale)
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
              AND (
                $search = ''
                OR EXISTS {
                  MATCH (s)-[:TILHOERER]->(c:Cluster)
                  WHERE toLower(coalesce(c.navn, '')) CONTAINS toLower($search)
                }
              )
            RETURN date(s.tidspunkt) AS date, count(DISTINCT s) AS conversations
            ORDER BY date ASC
            """;

    private final Neo4jClient neo4jClient;

    public SamtaleStatistikImpl(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @Override
    public OverblikTal hentOverblikstal(String companyId, LocalDateTime fra, LocalDateTime til, String search) {
        return neo4jClient.query(OVERBLIKSTAL)
                .bind(companyId).to("companyId")
                .bind(fra).to("fra")
                .bind(til).to("til")
                .bind(search).to("search")
                .fetchAs(OverblikTal.class)
                .mappedBy((typeSystem, row) -> new OverblikTal(
                        row.get("totalConversations").asLong(),
                        row.get("causesFound").asLong()))
                .one()
                .orElseThrow(() -> new IllegalStateException("Overbliksforespørgslen returnerede ingen resultatrække"));
    }

    @Override
    public List<CauseCount> hentTopAarsager(String companyId, LocalDateTime fra, LocalDateTime til, String search) {
        var resultater = neo4jClient.query(TOP_AARSAGER)
                .bind(companyId).to("companyId")
                .bind(fra).to("fra")
                .bind(til).to("til")
                .bind(search).to("search")
                .fetchAs(CauseCount.class)
                .mappedBy((typeSystem, row) -> new CauseCount(
                        row.get("id").asString(),
                        row.get("title").asString(""),
                        row.get("count").asLong()))
                .all();
        return new ArrayList<>(resultater);
    }

    @Override
    public List<DayCount> hentSamtalerPrDag(String companyId, LocalDateTime fra, LocalDateTime til, String search) {
        var resultater = neo4jClient.query(SAMTALER_PR_DAG)
                .bind(companyId).to("companyId")
                .bind(fra).to("fra")
                .bind(til).to("til")
                .bind(search).to("search")
                .fetchAs(DayCount.class)
                .mappedBy((typeSystem, row) -> new DayCount(
                        row.get("date").asLocalDate(),
                        row.get("conversations").asLong()))
                .all();
        return new ArrayList<>(resultater);
    }
}
