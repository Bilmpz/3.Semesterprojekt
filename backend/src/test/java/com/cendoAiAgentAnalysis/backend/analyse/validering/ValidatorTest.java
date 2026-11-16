package com.cendoAiAgentAnalysis.backend.analyse.validering;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ValidatorTest {

    private static void assertFejl(String kode, ValidationResult r) {
        assertFalse(r.ok());
        assertEquals(kode, r.fejlKode());
    }

    @Test
    void indlaesning() {
        IndlaesningValidator v = new IndlaesningValidator();
        assertTrue(v.validate("Kunde: Hej, jeg vil gerne booke en tid til service.").ok());
        assertFejl("tom_samtale", v.validate("   "));
        assertFejl("tom_samtale", v.validate(null));
        assertFejl("for_kort", v.validate("Hej"));
    }

    @Test
    void rensning() {
        RensningValidator v = new RensningValidator();
        assertTrue(v.validate("Kunde: Min bil larmer, kan I se på den?").ok());
        assertFejl("persondata_email", v.validate("Skriv til anna@mail.dk"));
        assertFejl("persondata_cpr", v.validate("Mit CPR er 010190-1234"));
        assertFejl("persondata_telefon", v.validate("Ring på 12 34 56 78"));
        assertFejl("persondata_telefon", v.validate("Ring på +4512345678"));
        assertFejl("tom_efter_rensning", v.validate(""));
    }

    @Test
    void aarsag() {
        AarsagValidator v = new AarsagValidator();
        assertTrue(v.validate("Spørgsmål om garanti").ok());
        assertFejl("forkert_laengde", v.validate("Garanti"));
        assertFejl("forkert_laengde", v.validate("Personen ville gerne høre noget mere om garantien på bilen"));
        assertFejl("indeholder_kunde", v.validate("Kunde ønsker specifik medarbejder"));
        assertFejl("ugyldig_json", v.validate("{\"primary_reason\": \"Garanti\"}"));
        assertFejl("tom_aarsag", v.validate(null));
    }

    @Test
    void embedding() {
        EmbeddingValidator v = new EmbeddingValidator(3);
        assertTrue(v.validate(new double[] {0.1, -0.2, 0.3}).ok());
        assertFejl("forkert_dimension", v.validate(new double[] {0.1, 0.2}));
        assertFejl("ugyldig_vaerdi", v.validate(new double[] {0.1, Double.NaN, 0.3}));
        assertFejl("nulvektor", v.validate(new double[] {0, 0, 0}));
        assertFejl("tom_vektor", v.validate(new double[0]));
    }

    @Test
    void reduktion() {
        ReduktionValidator v = new ReduktionValidator(2, 5);
        assertTrue(v.validate(new double[][] {{1, 2, 3, 4, 5}, {5, 4, 3, 2, 1}}).ok());
        assertFejl("forkert_antal_raekker", v.validate(new double[][] {{1, 2, 3, 4, 5}}));
        assertFejl("forkert_dimension", v.validate(new double[][] {{1, 2, 3, 4, 5}, {1, 2}}));
        assertFejl("ugyldig_vaerdi", v.validate(new double[][] {{1, 2, 3, 4, 5}, {1, 2, Double.NaN, 4, 5}}));
    }

    @Test
    void clustering() {
        ClusteringValidator v = new ClusteringValidator(6);
        assertTrue(v.validate(new int[] {0, 0, 1, 1, 2, -1}).ok());
        assertFejl("for_faa_clusters", v.validate(new int[] {0, 0, 0, 0, 0, 0}));
        assertFejl("for_meget_stoej", v.validate(new int[] {0, 1, -1, -1, -1, -1}));
        assertFejl("forkert_antal", v.validate(new int[] {0, 1}));
    }

    @Test
    void navngivning() {
        NavngivningValidator v = new NavngivningValidator();
        assertTrue(v.validate("Spørgsmål om garanti").ok());
        assertFejl("mangler_navn", v.validate(""));
        assertFejl("ugyldig_json", v.validate("{\"navn\": \"Garanti\"}"));
        assertFejl("for_langt_navn", v.validate("et alt for langt navn som slet ikke er en kort overskrift"));
    }

    @Test
    void navngivningAlleNavne() {
        Set<Integer> ids = Set.of(0, 1);
        assertTrue(NavngivningValidator.tjekAlleNavne(Map.of(0, "Garanti", 1, "Status på reparation"), ids).ok());
        assertFejl("mangler_navn", NavngivningValidator.tjekAlleNavne(Map.of(0, "Garanti"), ids));
        assertFejl("dublet_navn", NavngivningValidator.tjekAlleNavne(Map.of(0, "Garanti", 1, "garanti"), ids));
    }

    @Test
    void kraevGyldigKasterMedTrinOgKode() {
        AarsagValidator v = new AarsagValidator();
        assertEquals("Spørgsmål om garanti", v.kraevGyldig("Spørgsmål om garanti"));
        StepFailedException e = assertThrows(StepFailedException.class, () -> v.kraevGyldig("Garanti"));
        assertEquals("Årsagsudtrækning", e.trin());
        assertEquals("forkert_laengde", e.fejlKode());
    }
}
