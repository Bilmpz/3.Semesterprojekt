package com.cendoAiAgentAnalysis.backend.analyse.util;

/** Small vector helpers. */
public final class VectorUtils {

    private VectorUtils() {
    }

    /**
     * Scales every vector to length 1. After this, Euclidean distance (used by UMAP and HDBSCAN)
     * ranks points the same way as cosine similarity, which is how embeddings are meant to be compared.
     */
    public static double[][] l2Normalize(double[][] vectors) {
        double[][] result = new double[vectors.length][];
        for (int i = 0; i < vectors.length; i++) {
            double[] v = vectors[i];
            double norm = 0;
            for (double x : v) {
                norm += x * x;
            }
            norm = Math.sqrt(norm);
            double[] scaled = new double[v.length];
            for (int j = 0; j < v.length; j++) {
                scaled[j] = norm == 0 ? 0 : v[j] / norm;
            }
            result[i] = scaled;
        }
        return result;
    }
}
