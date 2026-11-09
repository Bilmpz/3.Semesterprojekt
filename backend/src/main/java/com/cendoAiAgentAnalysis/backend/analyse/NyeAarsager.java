// ID:          A6
// Delsystem:   Analyse
// Hvad:        Finder clusters, der er dukket op siden sidste kørsel (scenarie 2: nye mønstre).
package com.cendoAiAgentAnalysis.backend.analyse;

import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer; 
import com.cendoAiAgentAnalysis.backend.analyse.util.VectorUtils;     

import java.util.Collection;    // En samling af ting (fx en liste) – bruges til de gamle centroider
import java.util.HashSet;       // Et sæt uden dubletter – bruges til id'erne på nye clusters
import java.util.LinkedHashMap; // Et map, der husker rækkefølgen, tingene blev lagt ind i
import java.util.Map;           // Opslagstabel: nøgle -> værdi
import java.util.Set;           // Interface for sæt
import java.util.TreeMap;       // Et map, der holder nøglerne sorteret (0, 1, 2 ...)


public class NyeAarsager {
    // 0.8 betyder: ligner et nyt cluster et gammelt MINDRE end 80 %, er det nyt.
    public static final double STANDARD_GRAENSE = 0.8;
    // Den grænse, netop dette objekt bruger. Sættes én gang i konstruktøren.
    private final double graense;

    public NyeAarsager() {
        this(STANDARD_GRAENSE);
    }
    public NyeAarsager(double graense) {
        this.graense = graense;
    }

    /**
     * Beregner én centroide (gennemsnitsvektor) pr. cluster. Støj (-1) springes over.
     *
     * @param vektorer   én vektor pr. samtale (768 tal hver)
     * @param clusterIds cluster-id pr. samtale. Plads i i begge arrays = samme samtale
     * @return map: 
     */
    public Map<Integer, double[]> centroider(double[][] vektorer, int[] clusterIds) {

        // Sikkerhedstjek: er der 4 vektorer, skal der også være 4 cluster-id'er
        if (vektorer.length != clusterIds.length) {
            throw new IllegalArgumentException("Vektorer og clusterIds har forskellig længde");
        }

        // summer: cluster-id -> summen af alle vektorer i clusteret
        Map<Integer, double[]> summer = new TreeMap<>();
        // antal: cluster-id -> hvor mange samtaler clusteret har
        Map<Integer, Integer> antal = new TreeMap<>();

        // ---- Løkke 1: læg alle vektorer i hvert cluster sammen ----
        for (int i = 0; i < vektorer.length; i++) { // gå gennem samtalerne én ad gangen
            int id = clusterIds[i];                   // hvilket cluster hører samtale i til?

            if (id == Clusterer.NOISE) { // -1 = støj, hører ikke til noget cluster
                continue;              
            }

            // Hent summen for clusteret. Findes den ikke endnu, laves en ny
            // med lutter 0'er (samme længde som en vektor) og lægges i map'et.
            // k -> ... er en lambda: en lille funktion, der kun køres, hvis nøglen mangler.
            double[] sum = summer.computeIfAbsent(id, k -> new double[vektorer[0].length]);

            // Læg vektoren til summen, tal for tal (d = dimension: plads 0, 1, 2 ...)
            for (int d = 0; d < sum.length; d++) {
                sum[d] += vektorer[i][d];
            }

            // Tæl én op for clusteret.
            // merge: findes id'et ikke -> sæt til 1. Findes det -> gammel + 1.
            antal.merge(id, 1, Integer::sum);
        }

        //cluster-id -> centroide. LinkedHashMap husker rækkefølgen.
        Map<Integer, double[]> resultat = new LinkedHashMap<>();

        //  Løkke 2: lav summerne om til gennemsnit 
        for (var e : summer.entrySet()) {
            double[] gennemsnit = e.getValue(); // summen for clusteret
            int n = antal.get(e.getKey());      // antal samtaler i clusteret

            // Divider hvert tal med antallet -> gennemsnit. Fx [1.8, 0.6] / 2 = [0.9, 0.3]
            for (int d = 0; d < gennemsnit.length; d++) {
                gennemsnit[d] /= n;
            }

            // Normalisér til længde 1, så vi sammenligner RETNING og ikke længde.
            resultat.put(e.getKey(), VectorUtils.l2Normalize(new double[][]{gennemsnit})[0]);
        }

        return resultat; // fx { 0: [0.95, 0.32], 1: [0, 1] }
    }

    // Finder id'erne på de clusters, der ikke ligner noget gammelt cluster.
    public Set<Integer> findNye(Map<Integer, double[]> nye, Collection<double[]> gamle) {

        // Her samles id'erne på de nye clusters. Et Set har ingen dubletter.
        Set<Integer> nyeIds = new HashSet<>();

        // Første kørsel: ingen gamle at sammenligne med.
        // Ellers ville ALT være "nyt", og det er ikke brugbart -> returnér tomt sæt.
        if (gamle.isEmpty()) {
            return nyeIds;
        }

        // Gå gennem hvert nyt cluster. e.getKey() = id, e.getValue() = centroide
        for (var e : nye.entrySet()) {
            // Hvor meget ligner det nye cluster det gamle, det ligner MEST?
            // Er selv det under grænsen, er clusteret nyt.
            if (hoejesteLighed(e.getValue(), gamle) < graense) {
                nyeIds.add(e.getKey()); // gem id'et som "ny årsag"
            }
        }

        return nyeIds; // fx {1}
    }

    // Finder den højeste cosine similarity mellem én centroide og alle gamle.

    public double hoejesteLighed(double[] centroide, Collection<double[]> gamle) {

        double bedst = -1; // start med den lavest mulige lighed (cosine går fra -1 til 1)

        for (double[] g : gamle) { // sammenlign med hver gammel centroide
            // Math.max beholder den største værdi indtil nu
            bedst = Math.max(bedst, VectorUtils.cosine(centroide, g));
        }

        return bedst; // ligheden med det MEST lignende gamle cluster
    }
}