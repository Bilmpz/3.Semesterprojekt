package com.cendoAiAgentAnalysis.backend.analyse.naming;

import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;

import java.io.IOException;
import java.util.List;

/** Pipeline step 6: gives a cluster a human-readable name based on examples from it. */
public interface ClusterNamer {

    ClusterName name(List<String> examples) throws IOException, InterruptedException;
}
