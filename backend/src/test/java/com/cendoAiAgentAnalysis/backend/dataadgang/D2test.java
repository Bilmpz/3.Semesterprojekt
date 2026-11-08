package com.cendoAiAgentAnalysis.backend.dataadgang;

import com.cendoAiAgentAnalysis.backend.model.AiAgent;
import com.cendoAiAgentAnalysis.backend.model.Cluster;
import com.cendoAiAgentAnalysis.backend.model.Kunde;
import com.cendoAiAgentAnalysis.backend.model.Samtale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Tester D2's forespørgsler på testdata, som testen selv laver.
// @Transactional: alt, testen gemmer, bliver rullet tilbage bagefter, så databasen ikke rodes til.
// Testen bruger sin egen kunde ("test-kunde-d2"), så andres data (fx demo-data) ikke påvirker tallene.
@SpringBootTest
@Transactional
class D2test {

    @Autowired private ClusterRepository clusterRepository;
    @Autowired private SamtaleRepository samtaleRepository;

    // Perioden: hele oktober 2026
    private static final LocalDateTime OKT = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime NOV = LocalDateTime.of(2026, 11, 1, 0, 0);

    private Cluster ombooking;
    private Cluster reservedele;

    // Kører før HVER test: lægger de samme 8 testsamtaler ind som vores Cypher-script
    @BeforeEach
    void opretTestdata() {
        Kunde kundeA = new Kunde("test-kunde-d2", "D2 testkunde A");   // kun testen bruger den
        Kunde kundeB = new Kunde("test-kunde-d2-b", "D2 testkunde B");
        AiAgent agent = new AiAgent("test-agent-d2", "D2 testagent");

        ombooking = clusterRepository.save(new Cluster("Ombooking af tid", "Kunden vil flytte en tid", 4));
        reservedele = clusterRepository.save(new Cluster("Reservedele", "Kunden vil bestille dele", 3));

        gem("D2-T-1", kundeA, agent, ombooking,   LocalDateTime.of(2026, 10, 5, 10, 0));
        gem("D2-T-2", kundeA, agent, ombooking,   LocalDateTime.of(2026, 10, 12, 11, 0));
        gem("D2-T-3", kundeA, agent, ombooking,   LocalDateTime.of(2026, 10, 20, 9, 30));
        gem("D2-T-4", kundeA, agent, reservedele, LocalDateTime.of(2026, 10, 21, 14, 0));
        gem("D2-T-5", kundeA, agent, ombooking,   LocalDateTime.of(2026, 9, 28, 10, 0));  // fælde: september
        gem("D2-T-6", kundeA, agent, null,        LocalDateTime.of(2026, 10, 15, 16, 0)); // fælde: støj
        gem("D2-T-7", kundeB, agent, reservedele, LocalDateTime.of(2026, 10, 3, 8, 0));   // fælde: anden kunde
        gem("D2-T-8", kundeB, agent, reservedele, LocalDateTime.of(2026, 10, 9, 13, 0));  // fælde: anden kunde
    }

    // Hjælpemetode: laver og gemmer én samtale med dens pile
    private void gem(String id, Kunde kunde, AiAgent agent, Cluster cluster, LocalDateTime tidspunkt) {
        Samtale samtale = new Samtale(id, "testårsag", "testforklaring", tidspunkt);
        samtale.setKunde(kunde);
        samtale.setAiAgent(agent);
        samtale.setCluster(cluster); // null = støj, ingen pil
        samtaleRepository.save(samtale);
    }

    @Test // Overblikket: antal pr. cluster for kunde A i oktober
    void antalPrCluster() {
        List<ClusterAntal> svar = clusterRepository.antalSamtalerPrCluster("test-kunde-d2", OKT, NOV);

        assertThat(svar).hasSize(2);
        assertThat(svar.get(0).clusterNavn()).isEqualTo("Ombooking af tid");
        assertThat(svar.get(0).antal()).isEqualTo(3);
        assertThat(svar.get(1).clusterNavn()).isEqualTo("Reservedele");
        assertThat(svar.get(1).antal()).isEqualTo(1);
    }

    @Test // Et cluster: alle samtaler i Reservedele, nyeste først, uanset kunde
    void samtalerICluster() {
        List<Samtale> svar = samtaleRepository.findByClusterIdOrderByTidspunktDesc(reservedele.getId());

        assertThat(svar).extracting(Samtale::getId)
                .containsExactly("D2-T-4", "D2-T-8", "D2-T-7");
    }

    @Test // Kunde A's samtaler i oktober, nyeste først (med støj, uden september)
    void kundensSamtalerIPerioden() {
        List<Samtale> svar = samtaleRepository
                .findByKundeIdAndTidspunktBetweenOrderByTidspunktDesc("test-kunde-d2", OKT, NOV);

        assertThat(svar).extracting(Samtale::getId)
                .containsExactly("D2-T-4", "D2-T-3", "D2-T-6", "D2-T-2", "D2-T-1");
    }
}