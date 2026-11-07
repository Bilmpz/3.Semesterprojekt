package com.cendoAiAgentAnalysis.backend;

import com.cendoAiAgentAnalysis.backend.analyse.AnalyseService;
import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.model.TransferReason;
import com.cendoAiAgentAnalysis.backend.analyse.naming.ClusterNamer;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.DimensionReducer;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NatligAnalyseTest {
    @TempDir Path temporary;

    private PipelineConfig config(String... transcripts) throws Exception {
        Path file = temporary.resolve("input.xlsx");
        try (var book = new XSSFWorkbook()) {
            var sheet = book.createSheet("Model Input");
            String[] headers = {"conversation_id", "dataset_split", "tools_called", "synthetic_transcript"};
            var header = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) header.createCell(c).setCellValue(headers[c]);
            for (int i = 0; i < transcripts.length; i++) {
                var row = sheet.createRow(i + 1);
                String[] values = {"id" + i, "train", "transfer", transcripts[i]};
                for (int c = 0; c < values.length; c++) row.createCell(c).setCellValue(values[c]);
            }
            try (var out = Files.newOutputStream(file)) { book.write(out); }
        }
        var cfg = mock(PipelineConfig.class);
        when(cfg.datasetPath()).thenReturn(file);
        when(cfg.split()).thenReturn("train");
        when(cfg.mode()).thenReturn(PipelineConfig.PipelineMode.REASON_FIRST);
        return cfg;
    }

    private AnalyseService service(PipelineConfig cfg, ReasonExtractor extractor) throws Exception {
        Embedder embedder = texts -> List.of(new double[]{1, 0});
        DimensionReducer reducer = mock(DimensionReducer.class);
        when(reducer.reduce(any())).thenAnswer(call -> call.getArgument(0));
        Clusterer clusterer = mock(Clusterer.class);
        when(clusterer.cluster(any())).thenAnswer(call -> new int[((double[][]) call.getArgument(0)).length]);
        return new AnalyseService(cfg, extractor, embedder, reducer, clusterer, mock(ClusterNamer.class));
    }

    @Test void enSamtalefejlOgTomTekstStopperIkkeResten() throws Exception {
        var cfg = config("god", "fejl", "", "god igen");
        var service = service(cfg, text -> {
            if (text.equals("fejl")) throw new IOException("følsom tekst");
            return new TransferReason("booking", "forklaring");
        });
        var run = service.koerAnalyseMedDetaljer();
        assertEquals(4, run.inputCount());
        assertEquals(2, run.conversationErrors());
        assertEquals(List.of("id0", "id3"), run.result().conversations().stream().map(c -> c.id()).toList());
        assertEquals(2, run.embeddings().length);
        assertEquals(2, run.result().clusterIds().length);
    }

    @Test void laasenFrigivesEfterFilfejl() throws Exception {
        var cfg = config("god");
        var service = service(cfg, text -> new TransferReason("booking", ""));
        when(cfg.datasetPath()).thenReturn(temporary.resolve("mangler.xlsx"));
        assertThrows(IOException.class, service::koerAnalyse);
        when(cfg.datasetPath()).thenReturn(temporary.resolve("input.xlsx"));
        assertEquals(1, service.koerAnalyse().conversations().size());
    }

    @Test void samtidigeKoerslerAfvises() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var service = service(config("god"), text -> {
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
            return new TransferReason("booking", "");
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(service::koerAnalyse);
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertThrows(IllegalStateException.class, service::koerAnalyse);
            } finally { release.countDown(); }
            assertEquals(1, future.get(5, TimeUnit.SECONDS).conversations().size());
        }
    }

    @Test void alleUgyldigeGiverTomtResultat() throws Exception {
        var run = service(config(""), text -> { throw new AssertionError("Må ikke kaldes"); }).koerAnalyseMedDetaljer();
        assertEquals(1, run.conversationErrors());
        assertTrue(run.result().conversations().isEmpty());
    }
}
