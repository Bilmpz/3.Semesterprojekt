// ID:          W1
// Delsystem:   Web-API
// Hvad:        Overbliksdata for allerede viderestillede samtaler, årsager og udvikling.
// Se docs/Byggeplan_v0.6.docx

package com.cendoAiAgentAnalysis.backend.web.dto;

import java.time.LocalDate;
import java.util.List;

public record OverblikDto( //Record er en type i Java, der er designet til at holde data. Den er uforanderlig (immutable) og har automatisk genererede metoder som equals(), hashCode() og toString().
        int totalConversations, //Total antal samtaler vist i overbliks-siden.
        int causesFound, //Antal årsager(clusters) fundet i de filtrerede samtaler.
        boolean lowData, //Indikerer om der er lav datamængde, hvilket kan påvirke pålideligheden af analysen.
        List<CauseDto> topCauses, //Liste af de mest fremtrædende årsager, hver repræsenteret som et CauseDto-objekt.
        List<TimelinePointDto> timeline //Liste af datapunkter for tidslinjen, hver repræsenteret som et TimelinePointDto-objekt.
) {
    public record CauseDto(String id, String title, int count) {}

    public record TimelinePointDto(
            LocalDate date,
            int conversations
    ) {}
}