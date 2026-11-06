
package com.cendoAiAgentAnalysis.backend.analyse.output;

import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;
import com.cendoAiAgentAnalysis.backend.analyse.model.Conversation;
import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;
import com.cendoAiAgentAnalysis.backend.dataadgang.ClusterRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import com.cendoAiAgentAnalysis.backend.model.Cluster;
import com.cendoAiAgentAnalysis.backend.model.Samtale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class Neo4jResultWriterTest {

    private ClusterRepository clusterRepository;
    private SamtaleRepository samtaleRepository;
    private Neo4jResultWriter writer;

    @BeforeEach
    void setUp() {
        // Opretter falske repositories, så vi ikke bruger den rigtige database
        clusterRepository = mock(ClusterRepository.class);
        samtaleRepository = mock(SamtaleRepository.class);

        // Returnerer det cluster, som writeren forsøger at gemme
        when(clusterRepository.save(any(Cluster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Opretter writeren med de to repositories
        writer = new Neo4jResultWriter(
                clusterRepository,
                samtaleRepository
        );
    }

    @Test
    void shouldRejectNullResult() {
        // Kontrollerer at null giver en NullPointerException
        assertThrows(
                NullPointerException.class,
                () -> writer.write(null)
        );

        verifyNoInteractions(clusterRepository, samtaleRepository);
    }

    @Test
    void shouldRejectMismatchedResultCounts() {
        // Opretter et analyseresultat med forkert antal cluster-ID'er
        PipelineResult result = new PipelineResult(
                List.of(),
                List.of(),
                List.of(),
                new int[]{3},
                Map.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> writer.write(result)
        );

        verifyNoInteractions(clusterRepository, samtaleRepository);
    }

    @Test
    void shouldRejectMissingClusterName() {
        // Opretter en testsamtale
        Conversation conversation = new Conversation(
                "SYN-0001",
                "test",
                "Kunden kan ikke logge ind"
        );

        // Opretter et analyseresultat uden navn til cluster 5
        PipelineResult result = new PipelineResult(
                List.of(conversation),
                List.of("Loginproblem"),
                List.of("Kunden kan ikke logge ind på sin konto"),
                new int[]{5},
                Map.of()
        );

        // Kontrollerer at det manglende clusternavn giver den rigtige fejl
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> writer.write(result)
        );

        assertEquals(
                "Mangler oplysninger om cluster 5",
                exception.getMessage()
        );

        verifyNoInteractions(clusterRepository, samtaleRepository);
    }

    @Test
    void shouldAcceptValidPipelineResult() {
        // Opretter en testsamtale
        Conversation conversation = new Conversation(
                "SYN-0001",
                "test",
                "Kunden kan ikke logge ind"
        );

        // Opretter et cluster med navn og beskrivelse
        ClusterName clusterName = new ClusterName(
                "Loginproblemer",
                "Kunder der har problemer med at logge ind"
        );

        PipelineResult result = new PipelineResult(
                List.of(conversation),
                List.of("Loginproblem"),
                List.of("Kunden kan ikke logge ind på sin konto"),
                new int[]{5},
                Map.of(5, clusterName)
        );

        // Kontrollerer at resultatet kan behandles uden fejl
        assertDoesNotThrow(() -> writer.write(result));

        // Kontrollerer at ét cluster og én samtale gemmes
        verify(clusterRepository, times(1)).save(any(Cluster.class));
        verify(samtaleRepository, times(1)).save(any(Samtale.class));

        // Kontrollerer at samtalen er forbundet til et cluster
        ArgumentCaptor<Samtale> captor =
                ArgumentCaptor.forClass(Samtale.class);

        verify(samtaleRepository).save(captor.capture());

        Samtale savedConversation = captor.getValue();

        assertNotNull(savedConversation.getCluster());
    }

    @Test
    void shouldAcceptNoiseConversation() {
        // Opretter en samtale, der er klassificeret som støj
        Conversation conversation = new Conversation(
                "SYN-0002",
                "test",
                "Kunden har et usædvanligt spørgsmål"
        );

        PipelineResult result = new PipelineResult(
                List.of(conversation),
                List.of("Ukendt årsag"),
                List.of("Samtalen passer ikke ind i et almindeligt cluster"),
                new int[]{-1},
                Map.of()
        );

        // Støj må ikke kræve et clusternavn
        assertDoesNotThrow(() -> writer.write(result));

        // Støj skal gemmes som samtale, men uden cluster
        verify(clusterRepository, never()).save(any(Cluster.class));
        verify(samtaleRepository, times(1)).save(any(Samtale.class));

        ArgumentCaptor<Samtale> captor =
                ArgumentCaptor.forClass(Samtale.class);

        verify(samtaleRepository).save(captor.capture());

        Samtale savedConversation = captor.getValue();

        assertNull(savedConversation.getCluster());
    }
}
