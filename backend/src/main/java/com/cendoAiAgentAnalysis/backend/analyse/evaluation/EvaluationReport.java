package com.cendoAiAgentAnalysis.backend.analyse.evaluation;

import java.util.Map;

/**
 * How well the clusters match the answer key (transfer_classification).
 *
 * ARI (Adjusted Rand Index): 1 = identical grouping, 0 = no better than random.
 * NMI (Normalized Mutual Information): 1 = identical grouping, 0 = no shared information.
 * "All" treats noise as one extra group; "clustered" only looks at points that got a cluster.
 */
public record EvaluationReport(int clusterCount,
                               int trueClassCount,
                               double noiseShare,
                               double ariAll,
                               double nmiAll,
                               double ariClustered,
                               double nmiClustered,
                               Map<Integer, ClusterPurity> purityByCluster) {

    /** The most common true label inside one cluster and how dominant it is. */
    public record ClusterPurity(String majorityLabel, int majorityCount, int size) {
        public double share() {
            return size == 0 ? 0 : (double) majorityCount / size;
        }
    }
}
