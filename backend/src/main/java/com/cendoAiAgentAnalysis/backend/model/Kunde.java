// ID:          D1
// Delsystem:   Entitet
// Hvad:        Kunde fra klassediagrammet: id, navn. (Kunde)-[:HAR]->(Samtale), (Kunde)-[:HAR]->(AIAgent).
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.model;
import org.springframework.data.neo4j.core.schema.Node;

//@Node("Kunde") tells Spring Data Neo4j, that objects of this class represents nodes with the label Kunde
@Node("Kunde")
public class Kunde extends SamtaleDeltager {

      public Kunde() {
    }

    public Kunde(String id, String navn) {
        super(id, navn);
    }
}