// ID:          D2
// Delsystem:   Dataadgang
// Hvad:        Neo4jRepository<Cluster, String> + Cypher: antal samtaler pr. cluster for kunde og periode.
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.dataadgang; // Klassen ligger i mappen dataadgang

import com.cendoAiAgentAnalysis.backend.model.Cluster; // Henter din Cluster-klasse, så vi kan bruge den her
import org.springframework.data.neo4j.repository.Neo4jRepository; // Grund-interfacet fra Spring Data Neo4j

// Repository til Cluster-noder.
// Det er et interface UDEN kode: Spring laver selv en klasse bag det, når appen starter.
//   Cluster = typen af objekter, vi gemmer
//   String  = typen på id-feltet i Cluster
public interface ClusterRepository extends Neo4jRepository<Cluster, String>, ClusterStatistik {



}