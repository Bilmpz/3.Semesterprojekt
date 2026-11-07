// ID:          D2
// Delsystem:   Dataadgang
// Hvad:        Neo4jRepository<Samtale, String> + hent samtaler pr. kunde/periode og pr. cluster.
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.dataadgang; // Klassen ligger i mappen dataadgang

import com.cendoAiAgentAnalysis.backend.model.Samtale; // Din Samtale-klasse
import org.springframework.data.neo4j.repository.Neo4jRepository; // Grund-interfacet fra Spring Data Neo4j
import org.springframework.data.neo4j.repository.query.Query; // Til at skrive vores egen Cypher
import org.springframework.data.repository.query.Param; // Kobler $id i Cypher til parameteren

import java.time.LocalDateTime; // Dato + klokkeslæt til periodefilteret
import java.util.List; // Metoderne returnerer lister af samtaler

// Repository til Samtale-noder.
//   Samtale = typen af objekter, vi gemmer
//   String  = typen på id-feltet i Samtale
//   SamtaleStatistik = interface med metoder til statistik (hentOverblikstal)
public interface SamtaleRepository extends Neo4jRepository<Samtale, String>, SamtaleStatistik {

    // Alle samtaler i ét cluster, nyeste først (bruges, når medarbejderen åbner et cluster – UC2).
    //
    // Spring læser metodenavnet og laver selv forespørgslen:
    //   findBy               -> "find samtaler, hvor ..."
    //   ClusterId            -> samtale.cluster.id er lig parameteren (følger TILHOERER-pilen)
    //   OrderByTidspunktDesc -> sorter efter tidspunkt, nyeste først
    List<Samtale> findByClusterIdOrderByTidspunktDesc(String clusterId);

    // Alle samtaler for én kunde i en periode, nyeste først (dashboardets kunde- og tidsfilter).
    //
    //   findBy                  -> "find samtaler, hvor ..."
    //   KundeId                 -> samtale.kunde.id er lig kundeId (følger HAR-pilen)
    //   And                     -> og samtidig ...
    //   TidspunktBetween        -> tidspunkt ligger mellem fra og til (begge med)
    //   OrderByTidspunktDesc    -> nyeste først
    List<Samtale> findByKundeIdAndTidspunktBetweenOrderByTidspunktDesc(
            String kundeId, LocalDateTime fra, LocalDateTime til);

    // Navnet på den virksomhed, samtalen hører til (bruges som "company" i UC3 – W3).
    // Virksomheden ligger to pile væk: (Virksomhed)-[:HAR_KUNDE]->(Kunde)-[:HAR]->(Samtale).
    // Giver null, hvis samtalen ikke er koblet til en virksomhed.
    @Query("MATCH (v:Virksomhed)-[:HAR_KUNDE]->(:Kunde)-[:HAR]->(s:Samtale {id: $id}) RETURN v.navn LIMIT 1")
    String findVirksomhedsnavn(@Param("id") String id);
}
