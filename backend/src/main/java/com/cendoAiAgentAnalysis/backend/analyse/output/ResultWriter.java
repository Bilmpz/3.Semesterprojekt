package com.cendoAiAgentAnalysis.backend.analyse.output;

import com.cendoAiAgentAnalysis.backend.analyse.clustering.Clusterer;
import com.cendoAiAgentAnalysis.backend.analyse.evaluation.EvaluationReport;
import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;
import com.cendoAiAgentAnalysis.backend.analyse.model.PipelineResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Writes the results of a run:
 *  - assignments.csv : one line per conversation with the clustered text and the explanation
 *                      (opens in Excel; semicolon-separated)
 *  - clusters.json   : clusters with names, sizes and member ids (a good starting point for the dashboard)
 *  - summary.txt     : human-readable overview, incl. evaluation if enabled
 */
public final class ResultWriter {

    private ResultWriter() {
    }

    public static void write(Path dir, PipelineResult result, String settings, EvaluationReport report,
                             ObjectMapper mapper) throws IOException {
        Files.createDirectories(dir);
        writeAssignments(dir.resolve("assignments.csv"), result);
        writeClustersJson(dir.resolve("clusters.json"), result, mapper);
        Files.writeString(dir.resolve("summary.txt"), summary(result, settings, report), StandardCharsets.UTF_8);
    }

    public static String summary(PipelineResult result, String settings, EvaluationReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Indstillinger ===\n").append(settings).append("\n\n");

        Map<Integer, List<Integer>> members = result.membersByCluster();
        sb.append("=== Klynger ===\n");
        for (var entry : members.entrySet()) {
            int id = entry.getKey();
            List<Integer> idx = entry.getValue();
            sb.append('\n').append(label(id, result.names())).append(" (").append(idx.size()).append(" samtaler)\n");
            ClusterName name = result.names().get(id);
            if (name != null && !name.description().isEmpty()) {
                sb.append("  ").append(name.description()).append('\n');
            }
            if (report != null && report.purityByCluster().containsKey(id)) {
                var p = report.purityByCluster().get(id);
                sb.append(String.format("  Facit: %d af %d er '%s' (%.0f%%)%n",
                        p.majorityCount(), p.size(), p.majorityLabel(), 100 * p.share()));
            }
            idx.stream().limit(5).forEach(i -> {
                sb.append("  - ").append(result.conversations().get(i).id()).append(": ")
                        .append(oneLine(result.texts().get(i), 140)).append('\n');
                String explanation = result.explanations().get(i);
                if (!explanation.isEmpty() && !explanation.equals(result.texts().get(i))) {
                    sb.append("      ").append(oneLine(explanation, 160)).append('\n');
                }
            });
        }

        if (report != null) {
            sb.append("\n=== Evaluering mod facit (transfer_classification) ===\n");
            sb.append(String.format("Fundne klynger: %d   Sande årsager i data: %d   Andel støj: %.0f%%%n",
                    report.clusterCount(), report.trueClassCount(), 100 * report.noiseShare()));
            sb.append(String.format("ARI alle punkter: %.3f   NMI alle punkter: %.3f%n", report.ariAll(), report.nmiAll()));
            sb.append(String.format("ARI uden støj:    %.3f   NMI uden støj:    %.3f%n",
                    report.ariClustered(), report.nmiClustered()));
            sb.append("(1.0 = samme gruppering som facit, 0 = ikke bedre end tilfældigt)\n");
        }
        return sb.toString();
    }

    private static void writeAssignments(Path file, PipelineResult result) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('\uFEFF'); // BOM so Excel shows æ/ø/å correctly
            w.write("conversation_id;split;cluster_id;cluster_name;text;explanation\n");
            for (int i = 0; i < result.clusterIds().length; i++) {
                int id = result.clusterIds()[i];
                w.write(String.join(";",
                        result.conversations().get(i).id(),
                        result.conversations().get(i).split(),
                        id == Clusterer.NOISE ? "noise" : String.valueOf(id),
                        csv(label(id, result.names())),
                        csv(oneLine(result.texts().get(i), Integer.MAX_VALUE)),
                        csv(oneLine(result.explanations().get(i), Integer.MAX_VALUE))));
                w.write('\n');
            }
        }
    }

    private static void writeClustersJson(Path file, PipelineResult result, ObjectMapper mapper) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode clusters = root.putArray("clusters");
        for (var entry : result.membersByCluster().entrySet()) {
            int id = entry.getKey();
            ObjectNode c = clusters.addObject();
            c.put("id", id);
            c.put("noise", id == Clusterer.NOISE);
            ClusterName name = result.names().get(id);
            c.put("name", label(id, result.names()));
            c.put("description", name == null ? "" : name.description());
            c.put("size", entry.getValue().size());
            ArrayNode convs = c.putArray("conversations");
            for (int i : entry.getValue()) {
                convs.addObject()
                        .put("id", result.conversations().get(i).id())
                        .put("text", result.texts().get(i))
                        .put("explanation", result.explanations().get(i));
            }
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
    }

    private static String label(int id, Map<Integer, ClusterName> names) {
        if (id == Clusterer.NOISE) {
            return "Støj (passer ikke i nogen klynge)";
        }
        ClusterName name = names.get(id);
        return name == null ? "Klynge " + id : "Klynge " + id + ": " + name.name();
    }

    private static String oneLine(String s, int max) {
        String line = s.replace("\r", " ").replace("\n", " | ").strip();
        return line.length() > max ? line.substring(0, max) + "..." : line;
    }

    private static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
