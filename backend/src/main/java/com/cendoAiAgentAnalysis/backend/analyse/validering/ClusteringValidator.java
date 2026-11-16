package com.cendoAiAgentAnalysis.backend.analyse.validering;

import java.util.HashSet;
import java.util.Set;

/**
 * Trin 6: tjekker clustering-labels (én label pr. samtale, -1 = støj).
 * Fejl stopper hele kørslen.
 */
public class ClusteringValidator implements StepValidator<int[]> {

    public static final int STOEJ = -1;

    private final int forventetAntal;
    private final int minClusters;
    private final double maxStoejAndel;

    public ClusteringValidator(int forventetAntal) {
        this(forventetAntal, 2, 0.4);
    }

    public ClusteringValidator(int forventetAntal, int minClusters, double maxStoejAndel) {
        this.forventetAntal = forventetAntal;
        this.minClusters = minClusters;
        this.maxStoejAndel = maxStoejAndel;
    }

    @Override
    public ValidationResult validate(int[] labels) {
        if (labels == null || labels.length == 0) {
            return ValidationResult.fejl("ingen_labels", "Clustering gav ingen labels");
        }
        if (labels.length != forventetAntal) {
            return ValidationResult.fejl("forkert_antal",
                    labels.length + " labels, forventet " + forventetAntal);
        }
        Set<Integer> clusters = new HashSet<>();
        int stoej = 0;
        for (int label : labels) {
            if (label == STOEJ) {
                stoej++;
            } else {
                clusters.add(label);
            }
        }
        if (clusters.size() < minClusters) {
            return ValidationResult.fejl("for_faa_clusters",
                    clusters.size() + " clusters, mindst " + minClusters + " forventet");
        }
        double stoejAndel = (double) stoej / labels.length;
        if (stoejAndel > maxStoejAndel) {
            return ValidationResult.fejl("for_meget_stoej",
                    String.format("%.0f%% støj, højst %.0f%% tilladt", stoejAndel * 100, maxStoejAndel * 100));
        }
        return ValidationResult.gyldig();
    }

    @Override
    public String trin() {
        return "Clustering";
    }
}
