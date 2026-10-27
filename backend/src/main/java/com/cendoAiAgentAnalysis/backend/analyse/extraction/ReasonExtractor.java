package com.cendoAiAgentAnalysis.backend.analyse.extraction;

import com.cendoAiAgentAnalysis.backend.analyse.model.TransferReason;

import java.io.IOException;

/** Pipeline step 2: turns a whole transcript into a short primary reason plus an explanatory sentence. */
public interface ReasonExtractor {

    TransferReason extractReason(String transcript) throws IOException, InterruptedException;
}
