// ID:          W1
// Delsystem:   Web-API
// Hvad:        UC1 – GET /api/kunder og GET /api/virksomheder til filter-dropdownen.
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.web;

import com.cendoAiAgentAnalysis.backend.dataadgang.KundeRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.VirksomhedRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController //Denne klasse fungerer som en REST-controller, der modtager HTTP-requests og returnerer data
@RequestMapping("/api") //Angiver, at alle endpoints i denne controller vil have præfikset /api
public class OverblikController {
    private final KundeRepository kundeRepository;
    private final VirksomhedRepository virksomhedRepository;

    public OverblikController(KundeRepository kundeRepository, VirksomhedRepository virksomhedRepository) {
        this.kundeRepository = kundeRepository;
        this.virksomhedRepository = virksomhedRepository;
    }

    @GetMapping("/kunder")
    public List<KundeResponse> hentKunder() {
        return kundeRepository.findAll().stream() //Stream gør det muligt at behandle elementerne i en liste ét ad gangen.
                .map(kunde -> new KundeResponse(kunde.getId(), kunde.getNavn()))
                .toList();
    }

    @GetMapping("/virksomheder")
    public List<VirksomhedResponse> hentVirksomheder() {
        return virksomhedRepository.findAll().stream()
                .map(virksomhed -> new VirksomhedResponse(virksomhed.getId(), virksomhed.getNavn()))
                .toList();
    }

    public record KundeResponse(String id, String name) {
    }

    public record VirksomhedResponse(String id, String name) {
    }
}
