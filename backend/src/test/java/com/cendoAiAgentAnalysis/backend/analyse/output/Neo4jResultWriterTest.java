
package com.cendoAiAgentAnalysis.backend.analyse.output;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.cendoAiAgentAnalysis.backend.analyse.model.Conversation;

import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class Neo4jResultWriterTest {

    @Test
    void shouldRejectNullResult() {

        // Opretter den writer, vi vil teste
        Neo4jResultWriter writer = new Neo4jResultWriter();

        // Kontrollerer at null giver en NullPointerException
        assertThrows(NullPointerException.class, () -> writer.write(null));
    }
    @Test
    void shouldRejectMismatchedResultCounts() {

        // Opretter et analyseresultat med forkert antal cluster-ID'er
        PipelineResult result = new PipelineResult(
                List.of(),
                List.of(),
                List.of(),
                new int[]{3},
                java.util.Map.of()
        );
        Neo4jResultWriter writer = new Neo4jResultWriter();

        assertThrows(IllegalArgumentException.class, () -> writer.write(result));
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
                java.util.Map.of()
        );

        Neo4jResultWriter writer = new Neo4jResultWriter();

        // Kontrollerer at det manglende clusternavn giver den rigtige fejl
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> writer.write(result)
        );

        assertEquals(
                "Mangler oplysninger om cluster 5",
                exception.getMessage()
        );
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

        // Opretter et gyldigt analyseresultat
        PipelineResult result = new PipelineResult(
                List.of(conversation),
                List.of("Loginproblem"),
                List.of("Kunden kan ikke logge ind på sin konto"),
                new int[]{5},
                Map.of(5, clusterName)
        );

        Neo4jResultWriter writer = new Neo4jResultWriter();

        // Kontrollerer at resultatet kan behandles uden fejl
        assertDoesNotThrow(() -> writer.write(result));
    }
    @Test
    void shouldAcceptNoiseConversation() {
        // Opretter en samtale, der er klassificeret som støj
        Conversation conversation = new Conversation(
                "SYN-0002",
                "test",
                "Kunden har et usædvanligt spørgsmål"
        );

        // Cluster-ID -1 betyder støj
        PipelineResult result = new PipelineResult(
                List.of(conversation),
                List.of("Ukendt årsag"),
                List.of("Samtalen passer ikke ind i et almindeligt cluster"),
                new int[]{-1},
                Map.of()
        );

        Neo4jResultWriter writer = new Neo4jResultWriter();

        // Støj må ikke kræve et clusternavn
        assertDoesNotThrow(() -> writer.write(result));
    }
}
