package com.cendoAiAgentAnalysis.backend.dataadgang;

import java.time.LocalDateTime; // Dato + klokkeslæt
import java.util.List; // Liste af resultater

// Den del af ClusterRepository, som vi selv skriver koden til (i ClusterStatistikImpl).
public interface ClusterStatistik {

    // Antal samtaler pr. cluster for én kunde i en periode, største først.
    // fra er med, til er ikke med.
    List<ClusterAntal> antalSamtalerPrCluster(String kundeId, LocalDateTime fra, LocalDateTime til);


    // W2: antal samtaler pr. cluster for én VIRKSOMHED i en periode, med søgning på navnet.
    // companyId = "" betyder alle virksomheder. search = "" betyder ingen søgning.
    List<ClusterTal> hentClustersMedAntal(String companyId, LocalDateTime fra, LocalDateTime til, String search);

    // Én række i svaret: et cluster med navn, beskrivelse og antal samtaler i perioden
    record ClusterTal(String id, String navn, String beskrivelse, long antal) {
    }


    // W2/E2b: alle samtaler i ét cluster med virksomhedens navn, i ÉN forespørgsel (nyeste først).
    // Erstatter at slå virksomheden op for hver samtale (N+1-problemet fundet i svartidstesten).
    List<SamtaleRaekke> hentSamtalerICluster(String clusterId);

    // Én samtale med det, frontend skal bruge
    record SamtaleRaekke(String id, LocalDateTime tidspunkt, String virksomhed,
                         String transcript, String aarsag, String forklaring) {
    }

}