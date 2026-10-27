package com.cendoAiAgentAnalysis.backend.analyse.evaluation;

import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Compares the found clusters with the ground-truth transfer reasons from the Labels sheet.
 * Runs AFTER clustering; the labels are never used to build the clusters.
 */
public final class ClusterEvaluator {

    private ClusterEvaluator() {
    }

    public static EvaluationReport evaluate(int[] predicted, List<String> trueLabels) {
        int n = predicted.length;
        int[] truth = encode(trueLabels);

        // Only the points that ended up in a cluster
        List<Integer> clusteredIdx = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (predicted[i] != Clusterer.NOISE) {
                clusteredIdx.add(i);
            }
        }
        int[] predClustered = new int[clusteredIdx.size()];
        int[] truthClustered = new int[clusteredIdx.size()];
        for (int k = 0; k < clusteredIdx.size(); k++) {
            predClustered[k] = predicted[clusteredIdx.get(k)];
            truthClustered[k] = truth[clusteredIdx.get(k)];
        }

        Map<Integer, EvaluationReport.ClusterPurity> purity = new TreeMap<>();
        Map<Integer, Map<String, Integer>> counts = new HashMap<>();
        for (int i = 0; i < n; i++) {
            counts.computeIfAbsent(predicted[i], k -> new HashMap<>()).merge(trueLabels.get(i), 1, Integer::sum);
        }
        for (var entry : counts.entrySet()) {
            var best = entry.getValue().entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
            int size = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            purity.put(entry.getKey(), new EvaluationReport.ClusterPurity(best.getKey(), best.getValue(), size));
        }

        long clusterCount = java.util.Arrays.stream(predicted).filter(c -> c != Clusterer.NOISE).distinct().count();
        long classCount = trueLabels.stream().distinct().count();

        return new EvaluationReport(
                (int) clusterCount,
                (int) classCount,
                n == 0 ? 0 : (double) (n - clusteredIdx.size()) / n,
                adjustedRandIndex(predicted, truth),
                normalizedMutualInformation(predicted, truth),
                adjustedRandIndex(predClustered, truthClustered),
                normalizedMutualInformation(predClustered, truthClustered),
                purity);
    }

    private static int[] encode(List<String> labels) {
        Map<String, Integer> ids = new HashMap<>();
        int[] encoded = new int[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            encoded[i] = ids.computeIfAbsent(labels.get(i), k -> ids.size());
        }
        return encoded;
    }

    /** Contingency table: how many points have predicted label a and true label b. */
    private static Map<Long, Integer> contingency(int[] a, int[] b) {
        Map<Long, Integer> table = new HashMap<>();
        for (int i = 0; i < a.length; i++) {
            long key = ((long) a[i] << 32) ^ (b[i] & 0xffffffffL);
            table.merge(key, 1, Integer::sum);
        }
        return table;
    }

    private static Map<Integer, Integer> counts(int[] labels) {
        Map<Integer, Integer> c = new HashMap<>();
        for (int x : labels) {
            c.merge(x, 1, Integer::sum);
        }
        return c;
    }

    private static double choose2(double x) {
        return x * (x - 1) / 2.0;
    }

    static double adjustedRandIndex(int[] a, int[] b) {
        int n = a.length;
        if (n < 2) {
            return 0;
        }
        double sumCells = contingency(a, b).values().stream().mapToDouble(ClusterEvaluator::choose2).sum();
        double sumA = counts(a).values().stream().mapToDouble(ClusterEvaluator::choose2).sum();
        double sumB = counts(b).values().stream().mapToDouble(ClusterEvaluator::choose2).sum();
        double expected = sumA * sumB / choose2(n);
        double max = 0.5 * (sumA + sumB);
        return max == expected ? 1.0 : (sumCells - expected) / (max - expected);
    }

    static double normalizedMutualInformation(int[] a, int[] b) {
        int n = a.length;
        if (n == 0) {
            return 0;
        }
        Map<Integer, Integer> countA = counts(a);
        Map<Integer, Integer> countB = counts(b);
        double mi = 0;
        for (var cell : contingency(a, b).entrySet()) {
            int la = (int) (cell.getKey() >> 32);
            int lb = (int) (long) cell.getKey();
            double pij = (double) cell.getValue() / n;
            double pi = (double) countA.get(la) / n;
            double pj = (double) countB.get(lb) / n;
            mi += pij * Math.log(pij / (pi * pj));
        }
        double hA = entropy(countA, n);
        double hB = entropy(countB, n);
        double denom = (hA + hB) / 2.0;
        return denom == 0 ? 1.0 : mi / denom;
    }

    private static double entropy(Map<Integer, Integer> counts, int n) {
        double h = 0;
        for (int c : counts.values()) {
            double p = (double) c / n;
            h -= p * Math.log(p);
        }
        return h;
    }
}
