package com.cendoAiAgentAnalysis.backend.analyse.config;

import com.cendoAiAgentAnalysis.backend.analyse.model.Conversation;
import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;
import com.cendoAiAgentAnalysis.backend.analyse.output.Neo4jResultWriter;
import com.cendoAiAgentAnalysis.backend.dataadgang.ClusterRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import com.cendoAiAgentAnalysis.backend.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoWriterMetadataTest {
    private PipelineResult result() {
        return new PipelineResult(List.of(new Conversation("samtaler500-0001", "train", "Samtaletekst")),
                List.of("Pris på service"), List.of("Kunden spørger om prisen"),
                new int[]{-1}, Map.of());
    }

    @Test
    void preservesImportedCompanyRelationsAndTimestamp() {
        var repository = mock(SamtaleRepository.class);
        var date = LocalDateTime.of(2026, 11, 16, 10, 0);
        var conversation = new Samtale("samtaler500-0001", null, null, date);
        var customer = new Kunde("demo500-aunsbjerg-kunde", "Demo");
        var agent = new AiAgent("demo500-aunsbjerg-agent", "Demo");
        conversation.setKunde(customer);
        conversation.setAiAgent(agent);
        conversation.setStatus("AFVENTER_ANALYSE");
        when(repository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        new Neo4jResultWriter(mock(ClusterRepository.class), repository).write(result());
        var order = inOrder(repository);
        order.verify(repository).fjernClustertilknytninger(List.of("samtaler500-0001"));
        order.verify(repository).findById("samtaler500-0001");
        order.verify(repository).save(conversation);
        verify(repository).save(conversation);
        assertEquals(date, conversation.getTidspunkt());
        assertSame(customer, conversation.getKunde());
        assertSame(agent, conversation.getAiAgent());
        assertEquals("OK", conversation.getStatus());
        assertTrue(conversation.getViderestillet());
    }

    @Test
    void suppliesMissingTimestampForNewAnalysisResults() {
        var repository = mock(SamtaleRepository.class);
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        new Neo4jResultWriter(mock(ClusterRepository.class), repository).write(result());
        var captured = org.mockito.ArgumentCaptor.forClass(Samtale.class);
        verify(repository).save(captured.capture());
        assertNotNull(captured.getValue().getTidspunkt());
    }
}
