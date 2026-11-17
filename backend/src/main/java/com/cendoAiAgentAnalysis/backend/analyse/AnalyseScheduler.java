package com.cendoAiAgentAnalysis.backend.analyse;

import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.IOException;

/** Natjobbet bruger samme analyse-og-gem-forløb som den manuelle demoimport. */
@Component
public class AnalyseScheduler {
    private static final Logger log = LoggerFactory.getLogger(AnalyseScheduler.class);
    private final AnalyseOgGem analyse;
    private final PipelineConfig config;

    public AnalyseScheduler(AnalyseOgGem analyse, PipelineConfig config) {
        this.analyse = analyse;
        this.config = config;
    }

    @Scheduled(cron = "${analyse.cron:0 0 2 * * *}", zone = "Europe/Copenhagen")
    public void koerNatligAnalyse() {
        if (!"train".equals(config.split())) {
            log.warn("Natjob sprunget over: dataset.split skal være train");
            return;
        }
        try {
            analyse.koer();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Natlig analyse afbrudt");
        } catch (IOException | RuntimeException e) {
            log.error("Natlig analyse fejlede ({})", e.getClass().getSimpleName());
        }
    }
}
