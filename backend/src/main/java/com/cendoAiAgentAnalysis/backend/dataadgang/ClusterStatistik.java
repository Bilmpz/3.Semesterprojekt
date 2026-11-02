package com.cendoAiAgentAnalysis.backend.dataadgang;

import java.time.LocalDateTime; // Dato + klokkeslæt
import java.util.List; // Liste af resultater

// Den del af ClusterRepository, som vi selv skriver koden til (i ClusterStatistikImpl).
public interface ClusterStatistik {

    // Antal samtaler pr. cluster for én kunde i en periode, største først.
    // fra er med, til er ikke med.
    List<ClusterAntal> antalSamtalerPrCluster(String kundeId, LocalDateTime fra, LocalDateTime til);
}