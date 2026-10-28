// ID:          D1
// Delsystem:   Entitet
// Hvad:        AI-agent fra klassediagrammet: id, navn. (AIAgent)-[:HAANDTERER]->(Samtale).
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.model;
import org.springframework.data.neo4j.core.schema.Node;

@Node("AiAgent")
public class AiAgent extends SamtaleDeltager {

     public AiAgent() {
    }

    public AiAgent(String id, String navn) {
        super(id, navn);
    }
}