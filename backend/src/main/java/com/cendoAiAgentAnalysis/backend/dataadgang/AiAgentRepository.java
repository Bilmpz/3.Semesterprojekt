package com.cendoAiAgentAnalysis.backend.dataadgang;

// ID:          D2
// Delsystem:   Dataadgang
// Hvad:        Neo4jRepository<AiAgent, String>.
// Se docs/Byggeplan_v0.6.docx
import org.springframework.data.neo4j.repository.Neo4jRepository;
import com.cendoAiAgentAnalysis.backend.model.AiAgent; //makes the AiAgent class available in this file, so we can use it as a type parameter in the Neo4jRepository interface

public interface AiAgentRepository
        extends Neo4jRepository<AiAgent, String> {
}