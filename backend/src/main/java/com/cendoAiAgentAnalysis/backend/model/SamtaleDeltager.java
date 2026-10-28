package com.cendoAiAgentAnalysis.backend.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("SamtaleDeltager") //tells java that this calls represents a node in the Neo4j database with the label "SamtaleDeltager"
public abstract class SamtaleDeltager {
    @Id
    protected String id;
    protected String navn;

    //Empty constructor needed by Spring Data Neo4j
    protected SamtaleDeltager() {
    }

    protected SamtaleDeltager(String id, String navn) {
        this.id = id;
        this.navn = navn;
    }

    //I do not know yet if they should be public or protected. I will change them to protected for now, since they are not used outside of the package.
    protected void setId(String id) {  this.id = id; }
    protected void setNavn(String navn) { this.navn = navn; }

    public String getId() { return id; }
    public String getNavn() { return navn; }
}
