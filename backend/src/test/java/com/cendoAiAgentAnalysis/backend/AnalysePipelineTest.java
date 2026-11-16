
package com.cendoAiAgentAnalysis.backend;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


import com.cendoAiAgentAnalysis.backend.analyse.data.DatasetLoader;
import com.cendoAiAgentAnalysis.backend.analyse.model.Conversation;

import java.util.List;


import com.cendoAiAgentAnalysis.backend.analyse.AnalyseService;
import com.cendoAiAgentAnalysis.backend.analyse.config.PipelineConfig;
import com.cendoAiAgentAnalysis.backend.analyse.embedding.Embedder;
import com.cendoAiAgentAnalysis.backend.analyse.extraction.ReasonExtractor;
import com.cendoAiAgentAnalysis.backend.analyse.reduction.DimensionReducer;
import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.naming.ClusterNamer;

import static org.mockito.Mockito.*;


import static org.junit.jupiter.api.Assertions.*;

class AnalysePipelineTest {

    @TempDir
    Path tempDir;

    private Path createTestDataset() throws IOException {
        Path dataset = tempDir.resolve("pipeline-test.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Model Input");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("conversation_id");
            header.createCell(1).setCellValue("dataset_split");
            header.createCell(2).setCellValue("tools_called");
            header.createCell(3).setCellValue("synthetic_transcript");

            for (int i = 1; i <= 10; i++) {
                Row row = sheet.createRow(i);

                row.createCell(0).setCellValue("TEST-" + i);
                row.createCell(1).setCellValue("train");
                row.createCell(2).setCellValue("transfer");

                String transcript = i == 9
                        ? ""
                        : "Customer conversation number " + i;

                row.createCell(3).setCellValue(transcript);
            }

            try (var output = Files.newOutputStream(dataset)) {
                workbook.write(output);
            }
        }

        return dataset;
    }


    @Test
    void shouldCreateTenTestConversations() throws IOException {
        Path dataset = createTestDataset();

        assertTrue(Files.exists(dataset));

        try (var input = Files.newInputStream(dataset);
             XSSFWorkbook workbook = new XSSFWorkbook(input)) {

            Sheet sheet = workbook.getSheet("Model Input");

            assertNotNull(sheet);
            assertEquals(11, sheet.getPhysicalNumberOfRows());

            for (int i = 1; i <= 10; i++) {
                Row row = sheet.getRow(i);

                assertNotNull(row);
                assertEquals(
                        "TEST-" + i,
                        row.getCell(0).getStringCellValue()
                );
            }

            assertEquals(
                    "",
                    sheet.getRow(9).getCell(3).getStringCellValue()
            );
        }
    }


    @Test
    void shouldLoadTenConversationsIncludingEmptyTranscript() throws IOException {
        Path dataset = createTestDataset();

        List<Conversation> conversations =
                DatasetLoader.loadTransfers(dataset, "train");

        assertEquals(10, conversations.size());

        long emptyTranscripts = conversations.stream()
                .filter(conversation ->
                        conversation.transcript() == null
                                || conversation.transcript().isBlank())
                .count();

        assertEquals(1, emptyTranscripts);
    }

    private AnalyseService createTestService(Path dataset) throws IOException, InterruptedException {
        PipelineConfig config = mock(PipelineConfig.class);

        when(config.datasetPath()).thenReturn(dataset);
        when(config.split()).thenReturn("train");
        when(config.mode()).thenReturn(
                PipelineConfig.PipelineMode.FULL_TRANSCRIPT
        );
        when(config.namingEnabled()).thenReturn(false);

        ReasonExtractor extractor = mock(ReasonExtractor.class);
        Embedder embedder = mock(Embedder.class);
        DimensionReducer reducer = mock(DimensionReducer.class);
        Clusterer clusterer = mock(Clusterer.class);
        ClusterNamer namer = mock(ClusterNamer.class);


        when(embedder.embed(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);

            if (texts.getFirst().contains("number 10")) {
                throw new IOException("Simulated embedding failure");
            }

            return texts.stream()
                    .map(text -> new double[]{1.0, 2.0, 3.0})
                    .toList();
        });

        when(reducer.reduce(any(double[][].class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(clusterer.cluster(any(double[][].class)))
                .thenAnswer(invocation -> {
                    double[][] points = invocation.getArgument(0);
                    return new int[points.length];
                });

        return new AnalyseService(
                config,
                extractor,
                embedder,
                reducer,
                clusterer,
                namer
        );
    }

    @Test
    void shouldProcessEightConversationsAndCountTwoErrors() throws Exception {
        Path dataset = createTestDataset();
        AnalyseService service = createTestService(dataset);

        AnalyseService.AnalysisRun run =
                service.koerAnalyseMedDetaljer();

        assertEquals(10, run.inputCount());
        assertEquals(2, run.conversationErrors());

        assertEquals(8, run.result().conversations().size());
        assertEquals(8, run.result().texts().size());
        assertEquals(8, run.result().explanations().size());
        assertEquals(8, run.result().clusterIds().length);

        List<String> processedIds = run.result().conversations()
                .stream()
                .map(Conversation::id)
                .toList();

        assertFalse(processedIds.contains("TEST-9"));
        assertFalse(processedIds.contains("TEST-10"));
    }

}
