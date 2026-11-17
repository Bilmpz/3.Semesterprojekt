package com.cendoAiAgentAnalysis.backend.analyse.config;

import com.cendoAiAgentAnalysis.backend.analyse.AnalyseOgGem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Aktiveres eksplicit med Spring-profilen demo156. */
@Component
@ConditionalOnProperty(name = "analyse.import-on-startup", havingValue = "true")
public class DemoImportRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoImportRunner.class);
    private final DemoDatasetImporter importer;
    private final AnalyseOgGem analyse;
    private final boolean runAnalysis;

    public DemoImportRunner(DemoDatasetImporter importer, AnalyseOgGem analyse,
                            @Value("${analyse.run-on-startup:true}") boolean runAnalysis) {
        this.importer = importer;
        this.analyse = analyse;
        this.runAnalysis = runAnalysis;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Importerer syntetiske samtaler med demovirksomheder");
        int count = importer.importer();
        log.info("{} demosamtaler er gemt i Neo4j. Virksomheder og datoer er demodata.", count);
        if (runAnalysis) {
            log.info("Analyserer viderestillinger. Dette kan tage tid med lokale modeller.");
            var run = analyse.koer();
            log.info("{} viderestillinger analyseret og gemt; {} samtalefejl.",
                    run.result().conversations().size(), run.conversationErrors());
        }
    }
}
