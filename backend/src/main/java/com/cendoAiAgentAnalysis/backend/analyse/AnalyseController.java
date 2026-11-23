// ID:          A6 (kobling)
// Delsystem:   Analyse
// Hvad:        Starter en analysekørsel fra dashboardet ("Start ny kørsel") og viser, hvordan den gik.
package com.cendoAiAgentAnalysis.backend.analyse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/pipeline")
public class AnalyseController {

    private final AnalyseOgGem analyseOgGem;
    private final AtomicBoolean koerer = new AtomicBoolean(); // true, mens en kørsel er i gang
    private volatile String status = "Ingen kørsel endnu";

    public AnalyseController(AnalyseOgGem analyseOgGem) {
        this.analyseOgGem = analyseOgGem;
    }

    // POST /api/pipeline/run - kaldes af knappen "Start ny kørsel".
    // Kørslen tager lang tid, fordi LLM'en kører lokalt. Derfor starter den i baggrunden, og vi svarer med det samme.
    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> start() {
        if (!koerer.compareAndSet(false, true)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "En kørsel er allerede i gang"));
        }
        status = "Kører";
        new Thread(() -> {
            try {
                AnalyseService.AnalysisRun run = analyseOgGem.koer();
                status = "Færdig: " + run.inputCount() + " indlæst, " + run.result().conversations().size()
                        + " gemt, " + run.conversationErrors() + " sprunget over";
            } catch (Exception e) {
                // Kun fejltypen. Fejlteksten kan indeholde samtaletekst (designmål 1)
                status = "Fejlet: " + e.getClass().getSimpleName();
            } finally {
                koerer.set(false);
            }
        }, "analyse-koersel").start();
        return ResponseEntity.accepted().body(Map.of("status", "Kørslen er startet"));
    }

    // GET /api/pipeline/status - hvordan gik den seneste kørsel?
    @GetMapping("/status")
    public Map<String, String> hentStatus() {
        return Map.of("status", status);
    }
}
