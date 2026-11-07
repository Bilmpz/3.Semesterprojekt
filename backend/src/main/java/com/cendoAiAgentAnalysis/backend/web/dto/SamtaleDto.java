// ID:          W3
// Delsystem:   Web-API
// Hvad:        Én samtale: id, tidspunkt, kunde, transskription, årsag, forklaring. Matcher types.ts.
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.web.dto;

import java.time.LocalDateTime;

// Det JSON, frontend får, når en samtale åbnes (UC3).
// Feltnavnene skal være præcis de samme som Conversation i frontend/src/types.ts,
// ellers viser dashboardet ingenting. En record giver automatisk de rigtige JSON-navne.
public record SamtaleDto(
        String id,                // samtalens id, fx "SYN-0042"
        LocalDateTime timestamp,  // hvornår samtalen fandt sted (bliver til tekst som "2026-11-02T14:30:00")
        String company,           // navnet på virksomheden, samtalen hører til
        String transcript,        // transskriptionen – altid anonymiseret (designmål 1)
        String reason,            // den korte årsag fra LLM'en
        String explanation        // LLM'ens forklaring af den konkrete samtale
) {
}
