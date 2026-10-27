package com.cendoAiAgentAnalysis.backend.analyse.reduction;

import smile.manifold.UMAP;

/**
 * UMAP via Smile. Embeddings have ~1000 dimensions; in that many dimensions all points look roughly
 * equally far apart, which makes density-based clustering (HDBSCAN) struggle. UMAP keeps the
 * neighbourhood structure but squeezes it into a few dimensions.
 *
 * Note: Smile's UMAP needs the native OpenBLAS/ARPACK libraries (see README).
 */
public class UmapReducer implements DimensionReducer {

    private static final long SEED = 42;

    private final int dimensions;
    private final int neighbors;

    public UmapReducer(int dimensions, int neighbors) {
        this.dimensions = dimensions;
        this.neighbors = neighbors;
    }

    @Override
    public double[][] reduce(double[][] vectors) {
        trySetSeed();
        int k = Math.min(neighbors, vectors.length - 1);
        UMAP.Options options = new UMAP.Options(
                k,            // neighbours: how local/global the structure is
                dimensions,   // target dimensions
                500,          // epochs (500 is recommended for small datasets)
                1.0,          // learning rate (default)
                0.05,         // min distance: small = tighter clumps, which helps clustering
                1.0,          // spread (default)
                5,            // negative samples (default)
                1.0,          // repulsion strength (default)
                1.0);         // local connectivity (default)
        double[][] reduced = UMAP.fit(vectors, options);
        if (reduced.length != vectors.length) {
            throw new IllegalStateException("UMAP returnerede " + reduced.length + " punkter i stedet for "
                    + vectors.length + " (nogle punkter var ikke forbundet med resten)");
        }
        return reduced;
    }

    @Override
    public String description() {
        return "UMAP til " + dimensions + " dimensioner (" + neighbors + " naboer)";
    }

    /** Makes runs reproducible when Smile exposes a global seed. Silently skipped otherwise. */
    private static void trySetSeed() {
        try {
            Class.forName("smile.math.MathEx").getMethod("setSeed", long.class).invoke(null, SEED);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // No seed method in this Smile version: results may vary slightly between runs
        }
    }
}
