package com.cendoAiAgentAnalysis.backend.analyse;

import com.cendoAiAgentAnalysis.backend.analyse.NyeAarsager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NyeAarsagerTest {

    private final NyeAarsager nye = new NyeAarsager();

    @Test
    void centroidenErGennemsnittetAfClusteret() {
        double[][] v = { {1, 0}, {1, 0}, {0, 1} };
        int[] ids = { 0, 0, 1 };
        Map<Integer, double[]> c = nye.centroider(v, ids);
        assertArrayEquals(new double[]{1, 0}, c.get(0), 1e-9);
        assertArrayEquals(new double[]{0, 1}, c.get(1), 1e-9);
    }

    @Test
    void stoejFaarIngenCentroide() {
        double[][] v = { {1, 0}, {0, 1} };
        int[] ids = { 0, -1 };
        assertFalse(nye.centroider(v, ids).containsKey(-1));
    }

    @Test
    void foersteKoerselGiverIngenNye() {
        assertTrue(nye.findNye(Map.of(0, new double[]{1, 0}), List.of()).isEmpty());
    }

    @Test
    void clusterDerLignerEtGammeltErIkkeNyt() {
        Set<Integer> r = nye.findNye(Map.of(0, new double[]{0.99, 0.14}), List.of(new double[]{1, 0}));
        assertTrue(r.isEmpty());
    }

    @Test
    void clusterDerPegerEnAndenVejErNyt() {
        Map<Integer, double[]> c = Map.of(0, new double[]{1, 0}, 1, new double[]{0, 1});
        assertEquals(Set.of(1), nye.findNye(c, List.of(new double[]{1, 0})));
    }
}