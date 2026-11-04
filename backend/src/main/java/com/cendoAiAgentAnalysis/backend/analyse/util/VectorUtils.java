    /** Cosine similarity mellem to vektorer (1 = samme retning, 0 = intet til fælles). A6. */
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

    /** double[] -> List<Double>, så en vektor kan gemmes på en Neo4j-node. */
    public static java.util.List<Double> tilListe(double[] v) {
        java.util.List<Double> liste = new java.util.ArrayList<>(v.length);
        for (double x : v) liste.add(x);
        return liste;
    }

    /** List<Double> -> double[], når en vektor hentes fra Neo4j. */
    public static double[] tilArray(java.util.List<Double> liste) {
        double[] v = new double[liste.size()];
        for (int i = 0; i < v.length; i++) v[i] = liste.get(i);
        return v;
    }