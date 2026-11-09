// ID:          W3
// Delsystem:   Web-API
// Hvad:        UC3 – GET /api/samtaler/{id}. Control-klasse for UC3 (hentSamtale).
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.web;

import com.cendoAiAgentAnalysis.backend.analyse.data.Anonymizer;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import com.cendoAiAgentAnalysis.backend.model.Samtale;
import com.cendoAiAgentAnalysis.backend.web.dto.SamtaleDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController // Klassen modtager HTTP-requests og svarer med JSON
@RequestMapping("/api") // Alle endpoints i klassen starter med /api
public class SamtaleController {

    private final SamtaleRepository samtaleRepository;

    // Spring giver os selv repositoriet, når controlleren oprettes (dependency injection)
    public SamtaleController(SamtaleRepository samtaleRepository) {
        this.samtaleRepository = samtaleRepository;
    }

    // UC3: GET /api/samtaler/{id} – én konkret samtale, så medarbejderen selv kan vurdere grupperingen.
    // {id} i URL'en bliver til parameteren id, fx /api/samtaler/SYN-0042 -> id = "SYN-0042"
    @GetMapping("/samtaler/{id}")
    public SamtaleDto hentSamtale(@PathVariable String id) {
        // findById giver en Optional: enten samtalen eller ingenting.
        // Findes den ikke, kaster vi en 404, som FejlHaandtering laver om til { "fejl": "..." }
        Samtale samtale = samtaleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Samtalen med id " + id + " findes ikke"));

        // Virksomhedens navn ligger ikke på samtalen, men to pile væk: (Virksomhed)->(Kunde)->(Samtale)
        String virksomhed = samtaleRepository.findVirksomhedsnavn(id);

        return new SamtaleDto(
                samtale.getId(),
                samtale.getTidspunkt(),
                virksomhed != null ? virksomhed : "",
                // Teksten er allerede anonymiseret af A4, før den blev gemt.
                // Vi kører den igennem igen, så rå tekst aldrig kan slippe ud af API'et (designmål 1)
                Anonymizer.anonymize(samtale.getTranscript()),
                samtale.getViderestillingsaarsag(),
                samtale.getForklaring());
    }
}
