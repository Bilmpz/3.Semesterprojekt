package com.cendoAiAgentAnalysis.backend.analyse.embedding;

import java.io.IOException;
import java.util.List;

/** Pipeline step 3: turns texts into vectors. Returns one vector per input text, in the same order. */
public interface Embedder {

    List<double[]> embed(List<String> texts) throws IOException, InterruptedException;
}
