package com.cendoAiAgentAnalysis.backend.analyse.util;

import java.util.ArrayList;   
import java.util.List;     


/**
 * VectorUtils – små regne-hjælpere til vektorer.
 *
 * En vektor er en liste af tal, der beskriver hvad en samtale handler om.
 * Vektorerne bliver lavet af Embedder. Denne klasse regner bare på dem.
 *
 * l2Normalize – gør alle vektorer lige lange, så vi kun sammenligner
 *               hvad samtalerne handler om, ikke hvor lange de er.
 *
 * cosine      – giver et tal for hvor meget to vektorer ligner hinanden.
 *               1 = næsten samme årsag, 0 = intet til fælles.
 *               A6 bruger det til at se om et cluster er nyt.
 *               V1 bruger det til at finde lignende samtaler.
 *
 * tilListe    – laver vektoren om til en liste, fordi Neo4j kun kan gemme lister.
 *
 * tilArray    – laver listen tilbage til en vektor, når vi har hentet den
 *               fra Neo4j og skal regne på den igen.
 */



public final class VectorUtils {
    private VectorUtils() {
    }
    // Gør hver vektor (pil) til længde 1, men beholder retningen.
    // Så sammenligner UMAP og HDBSCAN kun retning, ligesom cosine.

    public static double[][] l2Normalize(double[][] vectors) {
    // Ind: mange vektorer (double[][]). Ud: mange vektorer, alle med længde 1.

        double[][] result = new double[vectors.length][];
        // Laver en tom "hylde" med plads til lige så mange vektorer som input.
        // [] til sidst = hver plads er tom endnu; den fyldes nedenfor.

        for (int i = 0; i < vectors.length; i++) {
            double[] v = vectors[i];
            double norm = 0;
            // norm skal blive vektorens længde. Starter på 0.
            for (double x : v) {
                norm += x * x;
            }
            // For hvert tal x i v: læg x² til norm.
            // Fx v = [3, 4] → norm = 9 + 16 = 25

            norm = Math.sqrt(norm);
            double[] scaled = new double[v.length];
            // Ny tom vektor med samme antal pladser som v
            for (int j = 0; j < v.length; j++) {
                scaled[j] = norm == 0 ? 0 : v[j] / norm;
            }
            // Del hvert tal med længden.
            // [3, 4] / 5 = [0.6, 0.8] → længden er nu præcis 1.
            // "norm == 0 ? 0 : ..." = hvis vektoren kun er nuller,
            // så skriv 0 (man må ikke dele med 0).

            result[i] = scaled;
        }
        return result;
    }
    // Hvor meget ligner to pile hinanden?
    public static double cosine(double[] a, double[] b) {
    // Ind: to vektorer. Ud: ét tal (ligheden).

        if (a.length != b.length) {
            throw new IllegalArgumentException("Vektorerne har forskellig længde");
        }
        // Har de ikke lige mange tal, kan de ikke sammenlignes.
        // throw = stop metoden med en fejl.

        double prik = 0, na = 0, nb = 0;
        // Tre tællere, alle starter på 0:
        // prik = prikproduktet, na = a's længde², nb = b's længde²

        for (int i = 0; i < a.length; i++) {
            prik += a[i] * b[i];   // gang tal på samme plads og læg sammen
            na += a[i] * a[i];     // a's tal i anden
            nb += b[i] * b[i];     // b's tal i anden
        }
        // Eksempel: a = [1, 0], b = [0.9, 0.1]
        // prik = 0.9   na = 1   nb = 0.82

        return (na == 0 || nb == 0) ? 0 : prik / (Math.sqrt(na) * Math.sqrt(nb));
        // Cosine-formlen: prik ÷ (længde a × længde b)
        // Eksempel: 0.9 / (1 × 0.906) = 0.99 → de ligner næsten 100 %
        // || betyder "eller": er en af pilene tom → svar 0 (undgå dele-med-0)
    }

    public static List<Double> tilListe(double[] v) {
        List<Double> liste = new ArrayList<>(v.length);
        // Ny tom liste. v.length = vi siger på forhånd, hvor stor den bliver
        // (bare en lille hastighedsting)
        for (double x : v) liste.add(x);
        // For hvert tal x i v: læg det i listen
        return liste;
    }
    public static double[] tilArray(List<Double> liste) {
        double[] v = new double[liste.size()];
        // Nyt array med lige så mange pladser, som listen har tal
        for (int i = 0; i < v.length; i++) v[i] = liste.get(i);
        // Kopiér tal for tal: plads i i listen → plads i i arrayet
        return v;
    }
}