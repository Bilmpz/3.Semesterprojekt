package com.cendoAiAgentAnalysis.backend.dataadgang;

import com.cendoAiAgentAnalysis.backend.model.Samtale;
import org.junit.jupiter.api.Test; // Markerer en testmetode
import org.springframework.beans.factory.annotation.Autowired; // Spring giver os objektet
import org.springframework.boot.test.context.SpringBootTest; // Starter Spring til testen

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat; // assertThat(x).isEqualTo(y) = "tjek at x er y"

// Tester D2's forespørgsler på testdataen, du lagde ind i Neo4j Browser (T-1 ... T-8).
// @SpringBootTest starter Spring og forbinder til den samme Neo4j som appen.
@SpringBootTest
class D2test {

    @Autowired private ClusterRepository clusterRepository; // Spring giver os det rigtige repository
    @Autowired private SamtaleRepository samtaleRepository;

    // Perioden: hele oktober 2026
    private static final LocalDateTime OKT = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime NOV = LocalDateTime.of(2026, 11, 1, 0, 0);

    @Test // Overblikket (UC1): antal pr. cluster for kunde 1 i oktober
    void antalPrCluster() {
        List<ClusterAntal> svar = clusterRepository.antalSamtalerPrCluster("1", OKT, NOV);

        assertThat(svar).hasSize(2); // To clusters
        assertThat(svar.get(0).clusterNavn()).isEqualTo("Ombooking af tid"); // Største først
        assertThat(svar.get(0).antal()).isEqualTo(3);
        assertThat(svar.get(1).clusterNavn()).isEqualTo("Reservedele");
        assertThat(svar.get(1).antal()).isEqualTo(1);
    }

    @Test // Et cluster (UC2): alle samtaler i Reservedele, nyeste først, uanset kunde
    void samtalerICluster() {
        List<Samtale> svar = samtaleRepository.findByClusterIdOrderByTidspunktDesc("test-c1");

        assertThat(svar).extracting(Samtale::getId) // Tag id'et fra hver samtale
                .containsExactly("T-4", "T-8", "T-7"); // 21/10, 9/10, 3/10
    }

    @Test // Kunde 1's samtaler i oktober, nyeste først (med støj, uden september)
    void kundensSamtalerIPerioden() {
        List<Samtale> svar = samtaleRepository
                .findByKundeIdAndTidspunktBetweenOrderByTidspunktDesc("1", OKT, NOV);

        assertThat(svar).extracting(Samtale::getId)
                .containsExactly("T-4", "T-3", "T-6", "T-2", "T-1");
    }
}