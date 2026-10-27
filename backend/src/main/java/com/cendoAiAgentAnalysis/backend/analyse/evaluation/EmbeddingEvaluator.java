package com.cendoAiAgentAnalysis.backend.analyse.evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluation that does not depend on HDBSCAN, so LLMs can be compared fairly on small datasets.
 *
 * - Nearest neighbour: for each text, is the most similar other text from the same true class?
 *   Measures how well the LLM's sentences separate the topics, without any clustering parameters.
 * - Fixed k: average-linkage clustering into exactly k clusters (k = number of true classes),
 *   so a run can never collapse into one big cluster.
 *
 * Both expect L2-normalised vectors, so the dot product is the cosine similarity.
 */
public final class EmbeddingEvaluator {

    private EmbeddingEvaluator() {
    }

    /** Share of points whose nearest other point (cosine) has the same true label. */
    public static double nearestNeighbourAccuracy(double[][] vectors, List<String> trueLabels) {
        int hits = 0;
        for (int i = 0; i < vectors.length; i++) {
            int best = -1;
            double bestSim = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < vectors.length; j++) {
                if (j == i) {
                    continue;
                }
                double sim = dot(vectors[i], vectors[j]);
                if (sim > bestSim) {
                    bestSim = sim;
                    best = j;
                }
            }
            if (best >= 0 && trueLabels.get(best).equals(trueLabels.get(i))) {
                hits++;
            }
        }
        return vectors.length == 0 ? 0 : (double) hits / vectors.length;
    }

    /**
     * Agglomerative clustering with average linkage on cosine distance, stopped at k clusters.
     * O(n^3), which is fine for a few hundred texts.
     */
    public static int[] averageLinkage(double[][] vectors, int k) {
        int n = vectors.length;
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                dist[i][j] = dist[j][i] = 1 - dot(vectors[i], vectors[j]);
            }
        }
        List<List<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            clusters.add(new ArrayList<>(List.of(i)));
        }
        while (clusters.size() > Math.max(1, k)) {
            int bestA = 0;
            int bestB = 1;
            double bestDist = Double.POSITIVE_INFINITY;
            for (int a = 0; a < clusters.size(); a++) {
                for (int b = a + 1; b < clusters.size(); b++) {
                    double d = averageDistance(clusters.get(a), clusters.get(b), dist);
                    if (d < bestDist) {
                        bestDist = d;
                        bestA = a;
                        bestB = b;
                    }
                }
            }
            clusters.get(bestA).addAll(clusters.remove(bestB));
        }
        int[] ids = new int[n];
        for (int c = 0; c < clusters.size(); c++) {
            for (int i : clusters.get(c)) {
                ids[i] = c;
            }
        }
        return ids;
    }

    private static double averageDistance(List<Integer> a, List<Integer> b, double[][] dist) {
        double sum = 0;
        for (int i : a) {
            for (int j : b) {
                sum += dist[i][j];
            }
        }
        return sum / (a.size() * b.size());
    }

    private static double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }
}
