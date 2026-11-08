// ID:          W2
// Delsystem:   Web-API
// Hvad:        UC2 – GET /api/clusters (alle årsager) og GET /api/clusters/{id}/samtaler. Control-klasse for UC2 (sekvensdiagram 5.2: hentCluster).
// Se docs/Byggeplan_v0.6.docx


package com.cendoAiAgentAnalysis.backend.web;

import com.cendoAiAgentAnalysis.backend.dataadgang.ClusterRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cendoAiAgentAnalysis.backend.dataadgang.ClusterStatistik.ClusterTal;
import com.cendoAiAgentAnalysis.backend.web.dto.ClusterDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import com.cendoAiAgentAnalysis.backend.analyse.data.Anonymizer;
import com.cendoAiAgentAnalysis.backend.model.Samtale;
import com.cendoAiAgentAnalysis.backend.web.dto.SamtaleDto;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.LocalDateTime;
import java.util.List;

@RestController         // Klassen modtager HTTP-requests og svarer med JSON
@RequestMapping("/api") // Alle endpoints i klassen starter med /api
public class ClusterController {

    private final ClusterRepository clusterRepository; // Til at hente clusters med antal (din Cypher)
    private final SamtaleRepository samtaleRepository; // Til at hente samtaler og Idas totaltal

    // Spring giver os selv repositories, når controlleren oprettes (dependency injection)
    public ClusterController(ClusterRepository clusterRepository, SamtaleRepository samtaleRepository) {
        this.clusterRepository = clusterRepository;
        this.samtaleRepository = samtaleRepository;
    }

    // UC2: GET /api/clusters?companyId=&days=&search=
    // Alle årsager (clusters) for en virksomhed i en periode, største først.
    @GetMapping("/clusters")
    public List<ClusterDto> hentClusters(
            @RequestParam(defaultValue = "") String companyId, // "" = alle virksomheder
            @RequestParam(defaultValue = "30") int days,       // perioden: de sidste X dage
            @RequestParam(defaultValue = "") String search) {  // "" = ingen søgning

        if (days < 1) { // En periode på 0 eller færre dage giver ikke mening
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days skal være mindst 1");
        }

        // Perioden: fra (nu - days) til nu
        LocalDateTime til = LocalDateTime.now();
        LocalDateTime fra = til.minusDays(days);

        // Hent clusters med antal samtaler (din Cypher fra bid 2)
        List<ClusterTal> clusters = clusterRepository.hentClustersMedAntal(companyId, fra, til, search);

        // Alle viderestillinger i perioden for virksomheden (Idas tal fra overblikket).
        // search = "" her: andelen skal regnes af ALLE viderestillinger, ikke kun dem der matcher søgningen.
        long total = samtaleRepository.hentOverblikstal(companyId, fra, til, "").totalConversations();


        // Til trend: del perioden i to lige store halvdele og tæl hvert cluster i hver halvdel
        LocalDateTime midt = fra.plus(Duration.between(fra, til).dividedBy(2));
        Map<String, Long> foersteHalvdel = antalPrCluster(companyId, fra, midt, search);
        Map<String, Long> andenHalvdel = antalPrCluster(companyId, midt, til, search);

        // Lav hvert ClusterTal om til en ClusterDto, som frontend forstår
        return clusters.stream()
                .map(c -> new ClusterDto(
                        c.id(),
                        c.navn(),                // title
                        c.beskrivelse(),         // description
                        c.antal(),               // count
                        andel(c.antal(), total), // share i hele procent
                        trend(foersteHalvdel.getOrDefault(c.id(), 0L),
                                andenHalvdel.getOrDefault(c.id(), 0L)))) // "up", "down" eller "flat")                 // trend: regnes ud i næste bid
                .toList();
    }


    // UC2: GET /api/clusters/{id}/samtaler – samtalerne i ét cluster ("Åbn →"), nyeste først.
    // {id} i URL'en bliver til parameteren id, fx /api/clusters/test-c0/samtaler -> id = "test-c0"
    @GetMapping("/clusters/{id}/samtaler")
    public List<SamtaleDto> hentSamtalerICluster(@PathVariable String id) {

        // Findes clusteret ikke, svarer vi 404. HC's FejlHaandtering laver det om til { "fejl": "..." }
        if (!clusterRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Clusteret med id " + id + " findes ikke");
        }

        // Din metode fra D2: alle samtaler i clusteret, nyeste først
        List<Samtale> samtaler = samtaleRepository.findByClusterIdOrderByTidspunktDesc(id);

        // Lav hver Samtale om til en SamtaleDto – samme måde som HC gør i SamtaleController
        return samtaler.stream()
                .map(s -> {
                    String virksomhed = samtaleRepository.findVirksomhedsnavn(s.getId());
                    return new SamtaleDto(
                            s.getId(),
                            s.getTidspunkt(),
                            virksomhed != null ? virksomhed : "",       // company
                            Anonymizer.anonymize(s.getTranscript()),   // transcript, altid anonymiseret
                            s.getViderestillingsaarsag(),              // reason
                            s.getForklaring());                        // explanation
                })
                .toList();
    }

    // Andel i hele procent: antal / total * 100, afrundet. Giver 0, hvis der ingen samtaler er.
    private long andel(long antal, long total) {
        if (total == 0) {
            return 0; // Undgå at dele med 0
        }
        return Math.round(antal * 100.0 / total);
    }


    // Antal samtaler pr. cluster-id i en periode, fx { "test-c0" -> 3, "test-c1" -> 1 }
    private Map<String, Long> antalPrCluster(String companyId, LocalDateTime fra, LocalDateTime til, String search) {
        return clusterRepository.hentClustersMedAntal(companyId, fra, til, search)
                .stream()
                .collect(Collectors.toMap(ClusterTal::id, ClusterTal::antal));
    }

    // Sammenlign første og anden halvdel: mere end 10 % flere = "up", mere end 10 % færre = "down"
    private String trend(long foer, long efter) {
        if (foer == 0) {
            return efter > 0 ? "up" : "flat"; // Fra ingenting til noget er en stigning
        }
        if (efter > foer * 1.10) {
            return "up";
        }
        if (efter < foer * 0.90) {
            return "down";
        }
        return "flat";
    }
}