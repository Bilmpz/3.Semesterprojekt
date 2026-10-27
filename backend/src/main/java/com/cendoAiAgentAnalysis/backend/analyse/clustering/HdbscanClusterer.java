package com.cendoAiAgentAnalysis.backend.analyse.clustering;

import smile.clustering.Clustering;
import smile.clustering.HDBSCAN;

/** Adapter for Smile's HDBSCAN (same call as in the earlier manual prototype). */
public class HdbscanClusterer implements Clusterer {

    private final int minPoints;
    private final int minClusterSize;

    public HdbscanClusterer(int minPoints, int minClusterSize) {
        this.minPoints = minPoints;
        this.minClusterSize = minClusterSize;
    }

    @Override
    public int[] cluster(double[][] points) {
        HDBSCAN<double[]> model = HDBSCAN.fit(points, minPoints, minClusterSize);
        int[] labels = model.group().clone();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == Clustering.OUTLIER) {
                labels[i] = NOISE;
            }
        }
        return labels;
    }

    @Override
    public String description() {
        return "HDBSCAN (minPoints=" + minPoints + ", minClusterSize=" + minClusterSize + ")";
    }
}
