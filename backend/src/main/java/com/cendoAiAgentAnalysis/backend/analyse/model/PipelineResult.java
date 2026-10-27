package com.cendoAiAgentAnalysis.backend.analyse.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Everything the pipeline produced for one run.
 *
 * @param conversations the input conversations
 * @param texts         the text that was embedded per conversation (primary reason or transcript)
 * @param explanations  the LLM's explanatory sentence per conversation ("" in pipeline 2); not embedded
 * @param clusterIds    cluster id per conversation (-1 = noise)
 * @param names         name per cluster id
 */
public record PipelineResult(List<Conversation> conversations,
                             List<String> texts,
                             List<String> explanations,
                             int[] clusterIds,
                             Map<Integer, ClusterName> names) {

    /** Indices of the conversations in each cluster, sorted by cluster id (noise = -1 first). */
    public Map<Integer, List<Integer>> membersByCluster() {
        Map<Integer, List<Integer>> members = new TreeMap<>();
        for (int i = 0; i < clusterIds.length; i++) {
            members.computeIfAbsent(clusterIds[i], k -> new ArrayList<>()).add(i);
        }
        return members;
    }

    /** The embedded text, followed by the explanation when there is one. */
    public String displayText(int i) {
        String explanation = explanations.get(i);
        return explanation.isEmpty() || explanation.equals(texts.get(i))
                ? texts.get(i)
                : texts.get(i) + " | " + explanation;
    }
}
