package com.cendoAiAgentAnalysis.backend.analyse.config;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class DemoDatasetImporterTest {
    @Test
    void trainSubsetMatchesTheOriginal156ConversationIds() throws Exception {
        var path = Path.of("src/main/java/com/cendoAiAgentAnalysis/backend/analyse/config/samtaler_156_pipeline.xlsx");
        var rows = DemoDatasetImporter.readRows(path, "train");
        var original = com.cendoAiAgentAnalysis.backend.analyse.data.DatasetLoader.loadTransfers(
                Path.of("data/syntetisk_voice_agent_dataset_analyseklart_500.xlsx"), "train");
        var expected = original.stream().map(c -> c.id().replace("SYN-", "samtaler500-"))
                .collect(Collectors.toSet());
        assertEquals(156, rows.size());
        assertEquals(expected, rows.stream().map(r -> r.get("id")).collect(Collectors.toSet()));
        assertTrue(rows.stream().allMatch(r -> Boolean.TRUE.equals(r.get("transfer"))));
    }
    private static final Path DATA = Path.of(
            "src/main/java/com/cendoAiAgentAnalysis/backend/analyse/config/samtaler_156_pipeline.xlsx");

    @Test
    void importsEveryConversationWithStableCompanyAndCorrectTransferFlag() throws Exception {
        var rows = DemoDatasetImporter.readRows(DATA, "train");
        assertEquals(156, rows.size());
        assertEquals(156, rows.stream().map(r -> r.get("id")).distinct().count());
        assertEquals(156, rows.stream().filter(r -> Boolean.TRUE.equals(r.get("transfer"))).count());
        Map<Object, Long> companies = rows.stream().collect(Collectors.groupingBy(
                r -> r.get("companyId"), Collectors.counting()));
        assertEquals(5, companies.size());
        assertEquals(java.util.Set.of(22L, 33L, 34L), new java.util.HashSet<>(companies.values()));
        assertEquals(rows, DemoDatasetImporter.readRows(DATA, "train"));
        assertTrue(rows.stream().noneMatch(r ->
                r.get("transcript").toString().matches("(?s).*transfer\\(\\{.*")));
    }

    @Test
    void refusesTestSplitBeforeImport() {
        assertThrows(IllegalArgumentException.class, () -> DemoDatasetImporter.readRows(DATA, "test"));
    }
}
