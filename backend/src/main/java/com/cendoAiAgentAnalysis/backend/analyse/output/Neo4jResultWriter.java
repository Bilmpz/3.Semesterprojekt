
package com.cendoAiAgentAnalysis.backend.analyse.output;

import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;
import java.util.Objects;
import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;

public class Neo4jResultWriter {


    public void write(PipelineResult result) {

        Objects.requireNonNull(result, "PipelineResult må ikke være null");
        int conversationCount = result.conversations().size();

        // Kontrollerer at alle samtaler har analyseresultater
        if (conversationCount != result.clusterIds().length
                || conversationCount != result.texts().size()
                || conversationCount != result.explanations().size()) {

            throw new IllegalArgumentException(
                    "Antallet af samtaler og analyseresultater stemmer ikke overens"
            );
        }

        for (int i = 0; i < conversationCount; i++) {

            var conversation = result.conversations().get(i);

            int clusterId = result.clusterIds()[i];
            // korte
            String reason = result.texts().get(i);
            // Lange forklaring
            String explanation = result.explanations().get(i);

            if (clusterId == -1) {
                // Samtalen er klassificeret som støj
                System.out.println("Samtale " + conversation.id() + " er støj");
            } else {
                // Samtalen tilhører et almindeligt cluster
                ClusterName clusterName = result.names().get(clusterId);
                if (clusterName == null) {
                    throw new IllegalArgumentException(
                            "Mangler oplysninger om cluster " + clusterId
                    );
                }
                String name = clusterName.name();
                String description = clusterName.description();
                System.out.println("Samtale " + conversation.id() + " tilhører cluster " + clusterId);
            }
        }
    }

}
