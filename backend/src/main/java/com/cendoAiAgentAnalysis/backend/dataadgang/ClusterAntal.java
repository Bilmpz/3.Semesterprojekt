package com.cendoAiAgentAnalysis.backend.dataadgang; // Ligger sammen med repositories

// Én række i svaret på "antal samtaler pr. cluster".
// Det er IKKE en node i databasen, bare en beholder til et resultat.
// "record" = Java laver selv konstruktør, getters (clusterId(), clusterNavn(), antal()) og toString
public record ClusterAntal(
        String clusterId,   // Clusterets id, så dashboardet kan åbne clusteret bagefter
        String clusterNavn, // Clusterets navn, fx "Ombooking af tid"
        long antal          // Antal samtaler i clusteret i perioden
) {
}