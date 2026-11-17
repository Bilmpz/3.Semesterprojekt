// ID:          W2
// Delsystem:   Web-API
// Hvad:        Én årsag: id, titel, beskrivelse, antal, andel, tendens. Matcher types.ts.
// Se docs/Byggeplan_v0.6.docx


package com.cendoAiAgentAnalysis.backend.web.dto;

// Det JSON, frontend får for ét cluster i "Alle årsager"-tabellen (UC2).
// Feltnavnene skal være præcis de samme som Cluster i frontend/src/types.ts,
// ellers viser dashboardet ingenting. En record giver automatisk de rigtige JSON-navne.
public record ClusterDto(
        String id,           // clusterets id i databasen, så frontend kan åbne det bagefter
        String title,        // clusterets navn, fx "Ombooking af tid"
        String description,  // clusterets beskrivelse fra LLM'en
        long count,          // antal samtaler i clusteret i den valgte periode
        double share,        // andel af alle viderestillinger i perioden, i procent (fx 0.4)
        String trend         // "up", "down" eller "flat"
) {
}
