package com.cendoAiAgentAnalysis.backend.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("Virksomhed")
public class Virksomhed {
    @Id
    private String id;
    private String navn;

    @Relationship(type = "HAR_KUNDE", direction = Relationship.Direction.OUTGOING)
    private List<Kunde> kunder = new ArrayList<>();

    @Relationship(type = "HAR_AI_AGENT", direction = Relationship.Direction.OUTGOING)
    private List<AiAgent> aiAgenter = new ArrayList<>();

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

    public List<Kunde> getKunder() {
        return List.copyOf(kunder);
    }

    public List<AiAgent> getAiAgenter() {
        return List.copyOf(aiAgenter);
    }

    public void setKunder(List<Kunde> kunder) {
        this.kunder = new ArrayList<>(kunder);
    }

    public void setAiAgenter(List<AiAgent> aiAgenter) {
        this.aiAgenter = new ArrayList<>(aiAgenter);
    }

    public void addKunde(Kunde kunde) {
        if (kunder.stream().noneMatch(eksisterende -> eksisterende.getId().equals(kunde.getId()))) {
            kunder.add(kunde);
        }
    }

    public void addAiAgent(AiAgent aiAgent) {
        if (aiAgenter.stream().noneMatch(eksisterende -> eksisterende.getId().equals(aiAgent.getId()))) {
            aiAgenter.add(aiAgent);
        }
    }
}
