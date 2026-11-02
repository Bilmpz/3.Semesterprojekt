package com.cendoAiAgentAnalysis.backend;

import com.cendoAiAgentAnalysis.backend.analyse.AnalyseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = AnalyseService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class AnalyseServiceTest {

    @Autowired
    private AnalyseService analyseService;

    @Test
    void kanIndlaeseSamtaler() throws Exception {
        analyseService.koerAnalyse();
    }
}