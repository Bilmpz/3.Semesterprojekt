package com.cendoAiAgentAnalysis.backend.analyse.config;

import com.cendoAiAgentAnalysis.backend.analyse.AnalyseOgGem;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DemoImportRunnerTest {
    @Test
    void canImportAllConversationsWithoutCallingModels() throws Exception {
        var importer = mock(DemoDatasetImporter.class);
        var analysis = mock(AnalyseOgGem.class);
        when(importer.importer()).thenReturn(156);
        new DemoImportRunner(importer, analysis, false).run();
        verify(importer).importer();
        verifyNoInteractions(analysis);
    }

    @Test
    void doesNotAnalyzeWhenDatabaseImportFails() throws Exception {
        var importer = mock(DemoDatasetImporter.class);
        var analysis = mock(AnalyseOgGem.class);
        when(importer.importer()).thenThrow(new IOException("import failed"));
        assertThrows(IOException.class, () -> new DemoImportRunner(importer, analysis, true).run());
        verifyNoInteractions(analysis);
    }
}
