// ID: V1 – Lignende samtaler
// GET /api/samtaler/{id}/lignende  ->  de 5 samtaler der ligner mest
package com.cendoAiAgentAnalysis.backend.web;

import com.cendoAiAgentAnalysis.backend.analyse.util.VectorUtils;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import com.cendoAiAgentAnalysis.backend.model.Samtale;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api")
public class LignendeController {
    // test
    
    // Det vi sender til frontend (aldrig transcript – GDPR)
    public record LignendeDto(String id, String aarsag, double lighed) {}

    private final SamtaleRepository repo;

    public LignendeController(SamtaleRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/samtaler/{id}/lignende")
    public List<LignendeDto> lignende(@PathVariable String id) {

        // 1. Hent samtalen – findes den ikke: 404
        Samtale valgt = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (valgt.getEmbedding() == null) return List.of();
        double[] minVektor = VectorUtils.tilArray(valgt.getEmbedding());

        // 2. Sammenlign med alle andre samtaler
        List<LignendeDto> resultat = new ArrayList<>();
        for (Samtale anden : repo.findAll()) {
            if (anden.getId().equals(id) || anden.getEmbedding() == null) continue;
            double lighed = VectorUtils.cosine(minVektor, VectorUtils.tilArray(anden.getEmbedding()));
            resultat.add(new LignendeDto(anden.getId(), anden.getViderestillingsaarsag(), lighed));
        }

        // 3. Mest lignende først, tag top 5
        resultat.sort((a, b) -> Double.compare(b.lighed(), a.lighed()));
        return resultat.stream().limit(5).toList();
    }
}
