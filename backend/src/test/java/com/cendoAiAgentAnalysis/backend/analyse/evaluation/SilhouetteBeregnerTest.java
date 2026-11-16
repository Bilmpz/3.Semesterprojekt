// ID:          E4
// Delsystem:   Analyse (test)
// Hvad:        Unit-test af SilhouetteBeregner: klumper ≈ 1, blandet ≈ 0, håndregnet eksempel, støj og ét cluster.
package com.cendoAiAgentAnalysis.backend.analyse.evaluation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SilhouetteBeregnerTest {

    @Test // To tydelige klumper langt fra hinanden -> score tæt på 1
    void toTydeligeKlumperGiverTaetPaaEn() {
        double[][] punkter = {
                {0.0, 0.0}, {0.1, 0.0}, {0.0, 0.1},       // klump 0 omkring (0, 0)
                {10.0, 10.0}, {10.1, 10.0}, {10.0, 10.1}  // klump 1 omkring (10, 10)
        };
        int[] ids = {0, 0, 0, 1, 1, 1};

        Map<Integer, Double> scorer = SilhouetteBeregner.perCluster(punkter, ids);

        assertEquals(2, scorer.size());
        assertTrue(scorer.get(0) > 0.9, "cluster 0: " + scorer.get(0));
        assertTrue(scorer.get(1) > 0.9, "cluster 1: " + scorer.get(1));
        assertTrue(SilhouetteBeregner.samlet(punkter, ids) > 0.9);
    }

    @Test // Punkterne ligger skiftevis i to "clusters", der i virkeligheden er det samme -> score omkring 0
    void blandedePunkterGiverTaetPaaNul() {
        double[][] punkter = new double[20][];
        int[] ids = new int[20];
        for (int i = 0; i < 20; i++) {
            punkter[i] = new double[]{i, 0.0}; // Alle på én linje
            ids[i] = i % 2;                    // 0, 1, 0, 1, ... skiftevis
        }

        double samlet = SilhouetteBeregner.samlet(punkter, ids);

        assertTrue(Math.abs(samlet) < 0.1, "samlet: " + samlet);
    }

    @Test // Lille eksempel regnet i hånden, så selve formlen tjekkes præcist
    void haandregnetEksempel() {
        // Fire punkter på en linje: A=0 og B=1 i cluster 0, C=4 og D=5 i cluster 1
        double[][] punkter = {{0}, {1}, {4}, {5}};
        int[] ids = {0, 0, 1, 1};

        // A: a = 1, b = (4 + 5) / 2 = 4.5   -> s = 3.5 / 4.5
        // B: a = 1, b = (3 + 4) / 2 = 3.5   -> s = 2.5 / 3.5
        // Cluster 0 = gennemsnittet af A og B. Cluster 1 er spejlvendt, så den giver det samme.
        double forventet = (3.5 / 4.5 + 2.5 / 3.5) / 2;

        Map<Integer, Double> scorer = SilhouetteBeregner.perCluster(punkter, ids);

        assertEquals(forventet, scorer.get(0), 1e-9);
        assertEquals(forventet, scorer.get(1), 1e-9);
        assertEquals(forventet, SilhouetteBeregner.samlet(punkter, ids), 1e-9);
    }

    @Test // Støjpunkter (-1) må ikke ændre resultatet
    void stoejTaellesIkkeMed() {
        double[][] uden = {{0}, {1}, {4}, {5}};
        int[] idsUden = {0, 0, 1, 1};

        double[][] med = {{0}, {1}, {4}, {5}, {2.5}, {100}}; // to ekstra støjpunkter
        int[] idsMed = {0, 0, 1, 1, -1, -1};

        assertEquals(SilhouetteBeregner.perCluster(uden, idsUden), SilhouetteBeregner.perCluster(med, idsMed));
        assertFalse(SilhouetteBeregner.perCluster(med, idsMed).containsKey(-1));
    }

    @Test // Med kun ét cluster er der intet at sammenligne med
    void kunEtClusterGiverTomtResultat() {
        double[][] punkter = {{0}, {1}, {2}, {50}};
        int[] ids = {0, 0, 0, -1};

        assertTrue(SilhouetteBeregner.perCluster(punkter, ids).isEmpty());
        assertTrue(Double.isNaN(SilhouetteBeregner.samlet(punkter, ids)));
    }

    @Test // Et punkt alene i sit cluster får 0 efter standarddefinitionen
    void punktAleneIClusterGiverNul() {
        double[][] punkter = {{0}, {1}, {10}};
        int[] ids = {0, 0, 1};

        assertEquals(0.0, SilhouetteBeregner.perCluster(punkter, ids).get(1), 1e-9);
    }

    @Test // Forkert input giver en tydelig fejl i stedet for et forkert tal
    void forkertAntalGiverFejl() {
        double[][] punkter = {{0}, {1}};
        int[] ids = {0};

        assertThrows(IllegalArgumentException.class, () -> SilhouetteBeregner.perCluster(punkter, ids));
    }
}
