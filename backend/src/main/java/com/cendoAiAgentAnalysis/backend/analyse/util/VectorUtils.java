package com.cendoAiAgentAnalysis.backend.analyse.util;

import java.util.ArrayList;
import java.util.List;

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

    /** A6: cosine similarity mellem to vektorer (1 = samme retning, 0 = intet til fælles). */
    public static double cosine(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vektorerne har forskellig længde");
        }
        double prik = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            prik += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0 : prik / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /** A6: double[] -> List<Double>, så en vektor kan gemmes på en Neo4j-node. */
    public static List<Double> tilListe(double[] v) {
        List<Double> liste = new ArrayList<>(v.length);
        for (double x : v) liste.add(x);
        return liste;
    }

    /** A6: List<Double> -> double[], når en vektor hentes fra Neo4j. */
    public static double[] tilArray(List<Double> liste) {
        double[] v = new double[liste.size()];
        for (int i = 0; i < v.length; i++) v[i] = liste.get(i);
        return v;
    }
}
