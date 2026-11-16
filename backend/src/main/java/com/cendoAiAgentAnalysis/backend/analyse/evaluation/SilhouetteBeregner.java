// ID:          E4
// Delsystem:   Analyse
// Hvad:        Silhouette-score pr. cluster på de 5-dim UMAP-punkter. Clusterkvalitet uden facit (MoSCoW: Should).
// Se Uge3-4_opgaver_v1.2
package com.cendoAiAgentAnalysis.backend.analyse.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Silhouette måler, hvor godt hvert punkt passer i sit eget cluster i forhold til det nærmeste andet cluster.
// For hvert punkt i:
//   a = gennemsnitlig afstand til de andre punkter i samme cluster
//   b = gennemsnitlig afstand til punkterne i det nærmeste ANDET cluster
//   s(i) = (b - a) / max(a, b)
// s er mellem -1 og 1: tæt på 1 = ligger godt i sit cluster, omkring 0 = på grænsen, negativ = passer bedre et andet sted.
// Et clusters score er gennemsnittet af s(i) for dets punkter.
//
// Regler:
//   - Støj (-1) tælles ikke med, hverken som cluster eller som nabo. HDBSCAN har selv sagt, at de punkter ikke hører til noget.
//   - Et punkt alene i sit cluster får s = 0 (standarddefinitionen).
//   - Med færre end 2 rigtige clusters kan der ikke beregnes noget: perCluster giver et tomt map, samlet giver NaN.
public final class SilhouetteBeregner {

    public static final int STOEJ = -1; // Samme værdi som Clusterer.NOISE

    private SilhouetteBeregner() { // Kun statiske metoder, så ingen objekter
    }

    // Silhouette pr. cluster: cluster-id -> gennemsnitlig score for clusterets punkter.
    public static Map<Integer, Double> perCluster(double[][] punkter, int[] clusterIds) {
        Map<Integer, List<Double>> scorerPrCluster = new TreeMap<>(); // TreeMap = sorteret efter cluster-id
        for (Map.Entry<Integer, Double> punkt : punktScorer(punkter, clusterIds).entrySet()) {
            int cluster = clusterIds[punkt.getKey()];
            scorerPrCluster.computeIfAbsent(cluster, k -> new ArrayList<>()).add(punkt.getValue());
        }
        Map<Integer, Double> resultat = new TreeMap<>();
        for (Map.Entry<Integer, List<Double>> e : scorerPrCluster.entrySet()) {
            resultat.put(e.getKey(), gennemsnit(e.getValue()));
        }
        return resultat;
    }

    // Samlet silhouette for hele kørslen: gennemsnit over alle punkter, der ikke er støj.
    // Det er det tal, der kan sammenlignes med tabel 7.10. Giver NaN, hvis der er færre end 2 clusters.
    public static double samlet(double[][] punkter, int[] clusterIds) {
        Map<Integer, Double> scorer = punktScorer(punkter, clusterIds);
        if (scorer.isEmpty()) {
            return Double.NaN;
        }
        return gennemsnit(new ArrayList<>(scorer.values()));
    }

    // s(i) for hvert punkt, der ikke er støj: punktets indeks -> score.
    // Tomt map, hvis der er færre end 2 rigtige clusters.
    private static Map<Integer, Double> punktScorer(double[][] punkter, int[] clusterIds) {
        tjekInput(punkter, clusterIds);

        // Saml punkternes indeks pr. cluster, uden støj
        Map<Integer, List<Integer>> medlemmer = new HashMap<>();
        for (int i = 0; i < clusterIds.length; i++) {
            if (clusterIds[i] != STOEJ) {
                medlemmer.computeIfAbsent(clusterIds[i], k -> new ArrayList<>()).add(i);
            }
        }
        Map<Integer, Double> scorer = new HashMap<>();
        if (medlemmer.size() < 2) {
            return scorer; // Silhouette kræver mindst to clusters at sammenligne med
        }

        for (Map.Entry<Integer, List<Integer>> eget : medlemmer.entrySet()) {
            for (int i : eget.getValue()) {
                if (eget.getValue().size() == 1) {
                    scorer.put(i, 0.0); // Alene i sit cluster
                    continue;
                }
                // a: gennemsnit til de ANDRE punkter i eget cluster (derfor size - 1)
                double a = sumAfstand(punkter, i, eget.getValue()) / (eget.getValue().size() - 1);

                // b: mindste gennemsnitsafstand til et andet cluster
                double b = Double.MAX_VALUE;
                for (Map.Entry<Integer, List<Integer>> andet : medlemmer.entrySet()) {
                    if (andet.getKey().equals(eget.getKey())) {
                        continue;
                    }
                    double gennemsnit = sumAfstand(punkter, i, andet.getValue()) / andet.getValue().size();
                    b = Math.min(b, gennemsnit);
                }

                double max = Math.max(a, b);
                scorer.put(i, max == 0 ? 0.0 : (b - a) / max); // Undgå at dele med 0, hvis alle punkter ligger oveni hinanden
            }
        }
        return scorer;
    }

    // Summen af afstande fra punkt i til punkterne i en gruppe (afstanden til sig selv er 0, så den gør ingen forskel)
    private static double sumAfstand(double[][] punkter, int i, List<Integer> gruppe) {
        double sum = 0;
        for (int j : gruppe) {
            sum += afstand(punkter[i], punkter[j]);
        }
        return sum;
    }

    // Euklidisk afstand: kvadratroden af summen af (x - y)^2 i hver dimension
    static double afstand(double[] x, double[] y) {
        double sum = 0;
        for (int d = 0; d < x.length; d++) {
            double forskel = x[d] - y[d];
            sum += forskel * forskel;
        }
        return Math.sqrt(sum);
    }

    private static double gennemsnit(List<Double> tal) {
        double sum = 0;
        for (double t : tal) {
            sum += t;
        }
        return sum / tal.size();
    }

    private static void tjekInput(double[][] punkter, int[] clusterIds) {
        if (punkter == null || clusterIds == null) {
            throw new IllegalArgumentException("Punkter og cluster-id'er må ikke være null");
        }
        if (punkter.length != clusterIds.length) {
            throw new IllegalArgumentException(
                    punkter.length + " punkter, men " + clusterIds.length + " cluster-id'er");
        }
        for (double[] punkt : punkter) {
            if (punkt == null || punkt.length != punkter[0].length) {
                throw new IllegalArgumentException("Alle punkter skal have samme antal dimensioner");
            }
        }
    }
}
