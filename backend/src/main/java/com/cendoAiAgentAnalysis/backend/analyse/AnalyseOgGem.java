// ID: A6 (kobling)




// Kører analysen og gemmer resultatet i Neo4j - Altså den analyseservice som jonathan har lavet. 
//Vi bruer det så i ligne samtaler og vectorutils 


package com.cendoAiAgentAnalysis.backend.analyse;

import com.cendoAiAgentAnalysis.backend.analyse.output.Neo4jResultWriter;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AnalyseOgGem {

    private final AnalyseService analyseService;
    private final Neo4jResultWriter writer;

    public AnalyseOgGem(AnalyseService analyseService, Neo4jResultWriter writer) {
        this.analyseService = analyseService;
        this.writer = writer;
    }

    // Kør hele pipelinen og gem. Returnerer kørslen, så kalderen kan logge tal.
    public AnalyseService.AnalysisRun koer() throws IOException, InterruptedException {
        AnalyseService.AnalysisRun run = analyseService.koerAnalyseMedDetaljer();
        // embeddings() er de L2-normaliserede 768-dim vektorer i samme rækkefølge som samtalerne
        writer.write(run.result(), run.embeddings());
        return run;
    }
}
