package com.cendoAiAgentAnalysis.backend.analyse.reduction;

/** Leaves the vectors unchanged. Useful as a baseline and as a fallback if UMAP cannot run. */
public class NoReduction implements DimensionReducer {

    @Override
    public double[][] reduce(double[][] vectors) {
        return vectors;
    }

    @Override
    public String description() {
        return "ingen dimensionsreduktion";
    }
}
