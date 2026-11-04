package com.cendoAiAgentAnalysis.backend.dataadgang;

import com.cendoAiAgentAnalysis.backend.model.Virksomhed;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface VirksomhedRepository extends Neo4jRepository<Virksomhed, String> {
}
