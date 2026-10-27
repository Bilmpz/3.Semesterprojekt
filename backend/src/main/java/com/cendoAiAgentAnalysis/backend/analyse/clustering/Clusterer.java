package com.cendoAiAgentAnalysis.backend.analyse.clustering;

/** Pipeline step 5: assigns every point a cluster id, or NOISE if it fits no cluster. */
public interface Clusterer {

    int NOISE = -1;

    int[] cluster(double[][] points);

    String description();
}
