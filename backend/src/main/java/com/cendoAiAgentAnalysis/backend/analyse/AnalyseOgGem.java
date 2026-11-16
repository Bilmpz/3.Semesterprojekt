// ID: A6 (kobling)




// Kører analysen og gemmer resultatet i Neo4j - Altså den analyseservice som jonathan har lavet. 
//Vi bruer det så i ligne samtaler og vectorutils 


package com.cendoAiAgentAnalysis.backend.analyse;

import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import com.cendoAiAgentAnalysis.backend.analyse.output.Neo4jResultWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnalyseOgGem {
    private static final Logger log = LoggerFactory.getLogger(AnalyseOgGem.class);

    // Datasættet har hverken tidspunkt, virksomhed eller agent. Dashboardet filtrerer på dem,
    // så nye samtaler får kørselstidspunktet og kobles til én virksomhed, én kunde og én AI-agent.
    private static final String KOBL_TIL_VIRKSOMHED = """
            MERGE (v:Virksomhed {id: 'datasaet-virksomhed'})
            ON CREATE SET v.navn = 'Værksted (syntetisk datasæt)'
            MERGE (k:Kunde:SamtaleDeltager {id: 'datasaet-kunde'})
            ON CREATE SET k.navn = 'Slutbrugere (datasæt)'
            MERGE (a:AiAgent:SamtaleDeltager {id: 'datasaet-agent'})
            ON CREATE SET a.navn = 'Værkstedsagent'
            MERGE (v)-[:HAR_KUNDE]->(k)
            MERGE (v)-[:HAR_AI_AGENT]->(a)
            WITH k, a
            MATCH (s:Samtale) WHERE s.id IN $ids
            SET s.tidspunkt = coalesce(s.tidspunkt, $nu)
            MERGE (k)-[:HAR]->(s)
            MERGE (a)-[:HAANDTERER]->(s)
            """;

    private final AnalyseService analyseService;
    private final Neo4jResultWriter writer;
    private final Neo4jClient neo4jClient;
    private final PipelineConfig config;

    public AnalyseOgGem(AnalyseService analyseService, Neo4jResultWriter writer,
                        Neo4jClient neo4jClient, PipelineConfig config) {
        this.analyseService = analyseService;
        this.writer = writer;
        this.neo4jClient = neo4jClient;
        this.config = config;
    }

    @Scheduled(cron = "${analyse.cron:0 0 2 * * *}", zone = "Europe/Copenhagen")
    public void koerNatligAnalyse() {
        try {
            // Drift må aldrig forbruge den urørte test-del.
            if (!"train".equals(config.split())) {
                log.warn("Natjob sprunget over: dataset.split skal være train");
                return;
            }
            koer();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Natlig analyse afbrudt");
        } catch (IOException | RuntimeException e) {
            // Undtagelsestekst kan indeholde modelsvar og samtaletekst.
            log.error("Natlig analyse fejlede ({})", e.getClass().getSimpleName());
        }
    }

    // Kør hele pipelinen og gem. Returnerer kørslen, så kalderen kan logge tal.
    public AnalyseService.AnalysisRun koer() throws IOException, InterruptedException {
        AnalyseService.AnalysisRun run = analyseService.koerAnalyseMedDetaljer();
        // embeddings() er de L2-normaliserede 768-dim vektorer i samme rækkefølge som samtalerne
        writer.write(run.result(), run.embeddings());
        List<String> ids = run.result().conversations().stream().map(c -> c.id()).toList();
        neo4jClient.query(KOBL_TIL_VIRKSOMHED)
                .bind(ids).to("ids")
                .bind(LocalDateTime.now()).to("nu")
                .run();
        return run;
    }
}
