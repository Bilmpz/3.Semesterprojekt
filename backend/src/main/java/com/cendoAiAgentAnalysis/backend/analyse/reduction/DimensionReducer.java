package com.cendoAiAgentAnalysis.backend.analyse.reduction;

/** Pipeline step 4: maps high-dimensional embeddings to fewer dimensions before clustering. */
public interface DimensionReducer {

    double[][] reduce(double[][] vectors);

    String description();
}
