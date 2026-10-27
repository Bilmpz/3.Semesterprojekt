package com.cendoAiAgentAnalysis.backend.analyse.embedding;

import com.cendoAiAgentAnalysis.backend.analyse.http.JsonHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Hugging Face's feature-extraction endpoint (API, requires credits). Sends texts in batches and
 * converts the JSON answer into double[] vectors.
 */
public class HuggingFaceEmbedder implements Embedder {

    private final JsonHttpClient http;
    private final String url;
    private final String model;
    private final int batchSize;

    public HuggingFaceEmbedder(JsonHttpClient http, String urlTemplate, String model, int batchSize) {
        this.http = http;
        this.url = urlTemplate.replace("{model}", model);
        this.model = model;
        this.batchSize = batchSize;
    }

    public String model() {
        return model;
    }

    @Override
    public List<double[]> embed(List<String> texts) throws IOException, InterruptedException {
        List<double[]> vectors = new ArrayList<>();
        for (int start = 0; start < texts.size(); start += batchSize) {
            List<String> batch = texts.subList(start, Math.min(start + batchSize, texts.size()));
            System.out.printf("  Embedder %d-%d af %d%n", start + 1, start + batch.size(), texts.size());

            ObjectNode body = http.mapper().createObjectNode();
            ArrayNode inputs = body.putArray("inputs");
            batch.forEach(inputs::add);

            JsonNode response = http.postJson(url, body);
            if (!response.isArray() || response.size() != batch.size()) {
                throw new IOException("Uventet svar fra embedding-API'et (forventede " + batch.size()
                        + " vektorer): " + response.toString().substring(0, Math.min(300, response.toString().length())));
            }
            for (JsonNode item : response) {
                vectors.add(toVector(item));
            }
        }
        return vectors;
    }

    /**
     * Sentence-embedding models return one vector per text: [0.1, 0.2, ...].
     * Some models instead return one vector per token: [[...], [...]]. Then we average them (mean pooling).
     */
    private static double[] toVector(JsonNode node) {
        if (node.size() > 0 && node.get(0).isArray()) {
            int dim = node.get(0).size();
            double[] mean = new double[dim];
            for (JsonNode token : node) {
                for (int j = 0; j < dim; j++) {
                    mean[j] += token.get(j).asDouble() / node.size();
                }
            }
            return mean;
        }
        double[] v = new double[node.size()];
        for (int j = 0; j < v.length; j++) {
            v[j] = node.get(j).asDouble();
        }
        return v;
    }
}
