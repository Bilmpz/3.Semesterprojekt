// ID:          D1
// Delsystem:   Entitet
// Hvad:        Samtale fra klassediagrammet: id, transcript, tidspunkt, embedding, viderestillingsaarsag, forklaring, status. (Samtale)-[:TILHOERER]->(Cluster).
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.model; // Klassen ligger i mappen model

import org.springframework.data.neo4j.core.schema.Id; // Markerer det unikke id
import org.springframework.data.neo4j.core.schema.Node; // Gør klassen til en node i Neo4j
import org.springframework.data.neo4j.core.schema.Relationship; // Gør et felt til en pil mellem to noder
import org.springframework.data.neo4j.core.schema.Relationship.Direction; // Hvilken vej pilen peger

import java.time.LocalDateTime; // Dato + klokkeslæt, fx 2026-11-02T14:30
import java.util.List; // Liste, bruges til embedding-vektoren

// Hvert Samtale-objekt bliver til én node med labelen "Samtale" i Neo4j
@Node("Samtale")
public class Samtale {

    // ---------------- Felter (bliver til properties på noden) ----------------

    @Id // Det unikke id
    private String id; // Samtalens id fra datasættet, fx "SYN-0042". Vi bruger det direkte,
    // så samme samtale ikke gemmes to gange, hvis pipelinen kører igen

    private String transcript; // Hele samtalen som tekst (anonymiseret af A4). Må være null,
    // fordi rå tekst skal kunne slettes efter et antal dage (GDPR)

    private LocalDateTime tidspunkt; // Hvornår samtalen fandt sted. Bruges til periodefilteret i dashboardet

    private List<Double> embedding; // Vektoren for årsagen fra embedding-modellen. Må være null

    private String viderestillingsaarsag; // Den korte årsag fra LLM'en, fx "Ønsker at tale med mekaniker"

    private String forklaring; // LLM'ens længere forklaring af den konkrete samtale

    private String status; // Om analysen gik godt, fx "OK" eller "FEJLET" (sættes af Validering, A5)

    // ---------------- Relationer (bliver til pile mellem noder) ----------------

    // (Samtale)-[:TILHOERER]->(Cluster)
    // OUTGOING = pilen går FRA samtalen UD til clusteret.
    // null = samtalen er støj (HDBSCAN gav -1), så får den ingen pil
    @Relationship(type = "TILHOERER", direction = Direction.OUTGOING)
    private Cluster cluster;

    // (Kunde)-[:HAR]->(Samtale)
    // INCOMING = pilen kommer FRA kunden IND til samtalen.
    // Feltet ligger her, så Kunde ikke skal have en (måske meget lang) liste af samtaler
    @Relationship(type = "HAR", direction = Direction.INCOMING)
    private Kunde kunde;

    // (AiAgent)-[:HAANDTERER]->(Samtale)
    // INCOMING igen: pilen kommer FRA agenten IND til samtalen
    @Relationship(type = "HAANDTERER", direction = Direction.INCOMING)
    private AiAgent aiAgent;

    // ---------------- Konstruktører ----------------

    // Tom konstruktør. Spring Data Neo4j bruger den, når den henter en samtale fra databasen
    protected Samtale() {
    }

    // Konstruktøren vi selv bruger (fx Mathias i Neo4jResultWriter).
    // De andre felter og relationerne sættes bagefter med setters, fordi de ikke altid findes
    public Samtale(String id, String viderestillingsaarsag, String forklaring, LocalDateTime tidspunkt) {
        this.id = id; // conversation.id()
        this.viderestillingsaarsag = viderestillingsaarsag; // result.texts().get(i)
        this.forklaring = forklaring; // result.explanations().get(i)
        this.tidspunkt = tidspunkt; // hvornår samtalen fandt sted
        this.status = "OK"; // en ny samtale starter som OK
    }

    // true, hvis samtalen ikke hører til noget cluster
    public boolean erStoej() {
        return cluster == null;
    }

    // ---------------- Getters ----------------

    public String getId() { return id; }
    public String getTranscript() { return transcript; }
    public LocalDateTime getTidspunkt() { return tidspunkt; }
    public List<Double> getEmbedding() { return embedding; }
    public String getViderestillingsaarsag() { return viderestillingsaarsag; }
    public String getForklaring() { return forklaring; }
    public String getStatus() { return status; }
    public Cluster getCluster() { return cluster; }
    public Kunde getKunde() { return kunde; }
    public AiAgent getAiAgent() { return aiAgent; }

    // ---------------- Setters ----------------

    public void setTranscript(String transcript) { this.transcript = transcript; }
    public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }
    public void setStatus(String status) { this.status = status; }
    public void setCluster(Cluster cluster) { this.cluster = cluster; } // laver TILHOERER-pilen ved save()
    public void setKunde(Kunde kunde) { this.kunde = kunde; } // laver HAR-pilen ved save()
    public void setAiAgent(AiAgent aiAgent) { this.aiAgent = aiAgent; } // laver HAANDTERER-pilen ved save()
}