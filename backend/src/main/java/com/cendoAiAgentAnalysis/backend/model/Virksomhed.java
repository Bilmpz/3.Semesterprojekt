package com.cendoAiAgentAnalysis.backend.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Virksomhed")
public class Virksomhed {
    @Id
    private String id;
    private String navn;

    public Virksomhed() {
    }

    public Virksomhed(String id, String navn) {
        this.id = id;
        this.navn = navn;
    }

    public String getId() {
        return id;
    }

    public String getNavn() {
        return navn;
    }
}
