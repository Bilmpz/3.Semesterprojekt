// ID:          D1
// Delsystem:   Entitet
// Hvad:        Cluster fra klassediagrammet: id, navn/label, beskrivelse, størrelse.
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.model; // Klassen ligger i mappen model

import org.springframework.data.neo4j.core.schema.GeneratedValue; // Lader databasen lave id'et selv
import org.springframework.data.neo4j.core.schema.Id; // Markerer det unikke id
import org.springframework.data.neo4j.core.schema.Node; // Gør klassen til en node i Neo4j
import org.springframework.data.neo4j.core.support.UUIDStringGenerator; // Laver unikke tekst-id'er, fx "3f2a9c1e-..."

// Hvert Cluster-objekt bliver til én node med labelen "Cluster" i Neo4j
@Node("Cluster")
public class Cluster {

    @Id // Det her felt er nodens unikke id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class) // Id'et laves automatisk, første gang clusteret gemmes
    private String id; // Er null, indtil clusteret er gemt

    private String navn; // Kort navn fra LLM'en, fx "Ombooking af tid"

    private String beskrivelse; // Én sætning fra LLM'en, der forklarer clusteret

    private int stoerrelse; // Antal samtaler i clusteret

    // Tom konstruktør. Spring Data Neo4j bruger den, når den henter et cluster fra databasen
    protected Cluster() {
    }

    // Konstruktøren vi selv bruger, når pipelinen har fundet et nyt cluster.
    // Id'et er ikke med, fordi databasen laver det ved save()
    public Cluster(String navn, String beskrivelse, int stoerrelse) {
        this.navn = navn; // Gem navnet i feltet
        this.beskrivelse = beskrivelse; // Gem beskrivelsen i feltet
        this.stoerrelse = stoerrelse; // Gem størrelsen i feltet
    }

    // Getters: lader andre klasser læse felterne
    public String getId() { return id; }

    public String getNavn() { return navn; }

    public String getBeskrivelse() { return beskrivelse; }

    public int getStoerrelse() { return stoerrelse; }

    // Setters: lader andre klasser ændre felterne (fx hvis et cluster omdøbes)
    public void setNavn(String navn) { this.navn = navn; }

    public void setBeskrivelse(String beskrivelse) { this.beskrivelse = beskrivelse; }

    public void setStoerrelse(int stoerrelse) { this.stoerrelse = stoerrelse; }
}