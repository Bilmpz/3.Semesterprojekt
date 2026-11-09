package com.cendoAiAgentAnalysis.backend;

import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import com.cendoAiAgentAnalysis.backend.model.Samtale;
import com.cendoAiAgentAnalysis.backend.web.LignendeController;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LignendeControllerTest {

    // Lille hjaelper: laver en samtale med en vektor
    private Samtale samtale(String id, Double... vektor) {
        Samtale s = new Samtale(id, "Aarsag " + id, "", null);
        s.setEmbedding(List.of(vektor));
        return s;
    }

    @Test
    void mestLignendeKommerFoerst() {
        Samtale a = samtale("a", 1.0, 0.0);
        Samtale b = samtale("b", 0.9, 0.1); // ligner a meget
        Samtale c = samtale("c", 0.0, 1.0); // ligner a slet ikke

        SamtaleRepository repo = mock(SamtaleRepository.class); // falsk database
        when(repo.findById("a")).thenReturn(Optional.of(a));
        when(repo.findAll()).thenReturn(List.of(a, b, c));

        var svar = new LignendeController(repo).lignende("a");

        assertEquals(2, svar.size());          // a selv er ikke med
        assertEquals("b", svar.get(0).id());   // b er mest lignende
    }

    @Test
    void ukendtIdGiver404() {
        SamtaleRepository repo = mock(SamtaleRepository.class);
        when(repo.findById("x")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> new LignendeController(repo).lignende("x"));
    }
}
