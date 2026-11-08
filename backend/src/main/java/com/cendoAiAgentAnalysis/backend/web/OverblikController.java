// ID:          W1
// Delsystem:   Web-API
// Hvad:        UC1 – GET /api/kunder og GET /api/virksomheder til filter-dropdownen.
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.web;

import com.cendoAiAgentAnalysis.backend.dataadgang.KundeRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleStatistik.OverblikTal;
import com.cendoAiAgentAnalysis.backend.dataadgang.VirksomhedRepository;
import com.cendoAiAgentAnalysis.backend.web.dto.OverblikDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController //Denne klasse fungerer som en REST-controller, der modtager HTTP-requests og returnerer data
@RequestMapping("/api") //Angiver, at alle endpoints i denne controller vil have præfikset /api
public class OverblikController {
    private final KundeRepository kundeRepository;
    private final VirksomhedRepository virksomhedRepository;
    private final SamtaleRepository samtaleRepository;

    public OverblikController(
            KundeRepository kundeRepository,
            VirksomhedRepository virksomhedRepository,
            SamtaleRepository samtaleRepository) {
        this.kundeRepository = kundeRepository;
        this.virksomhedRepository = virksomhedRepository;
        this.samtaleRepository = samtaleRepository;
    }

    //Håndterer GET-requests til /api/kunder og returnerer en liste af kunder som JSON.
    @GetMapping("/kunder")
    public List<KundeResponse> hentKunder() {
        return kundeRepository.findAll().stream() //Stream gør det muligt at behandle elementerne i en liste ét ad gangen.
                .map(kunde -> new KundeResponse(kunde.getId(), kunde.getNavn()))
                .toList();
    }

    //Håndterer GET-requests til /api/virksomheder og returnerer en liste af virksomheder som JSON.
    @GetMapping("/virksomheder")
    public List<VirksomhedResponse> hentVirksomheder() {
        return virksomhedRepository.findAll().stream()
                .map(virksomhed -> new VirksomhedResponse(virksomhed.getId(), virksomhed.getNavn()))
                .toList();
    }

    //Håndterer GET-requests til /api/overblik og returnerer et OverblikDto-objekt som JSON.
    @GetMapping("/overblik")
    public OverblikDto hentOverblik( //Public så spring kan tilgå metoden, retunerer et OverblikDto-objekt, som indeholder data til overbliks-siden.
            @RequestParam(defaultValue = "") String companyId, //frontend kan sende værdier med i URL'en
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "") String search) {
        if (days < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days skal være mindst 1");
        }

        LocalDateTime til = LocalDateTime.now();
        LocalDateTime fra = til.minusDays(days);
        OverblikTal tal = samtaleRepository.hentOverblikstal(companyId, fra, til, search);
        List<OverblikDto.CauseDto> topCauses = samtaleRepository.hentTopAarsager(companyId, fra, til, search)
                .stream()
                .map(cause -> new OverblikDto.CauseDto(
                        cause.id(),
                        cause.title(),
                        Math.toIntExact(cause.count())))
                .toList();
        List<OverblikDto.TimelinePointDto> timeline = samtaleRepository.hentSamtalerPrDag(companyId, fra, til, search)
                .stream()
                .map(day -> new OverblikDto.TimelinePointDto(
                        day.date(),
                        Math.toIntExact(day.conversations())))
                .toList();

        return new OverblikDto(
                Math.toIntExact(tal.totalConversations()),
                Math.toIntExact(tal.causesFound()),
                tal.totalConversations() < 20,
                topCauses,
                timeline);
    }

    public record KundeResponse(String id, String name) {
    }

    public record VirksomhedResponse(String id, String name) {
    }
}
