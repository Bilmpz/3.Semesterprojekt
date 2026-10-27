package com.cendoAiAgentAnalysis.backend.analyse.embedding;

import com.cendoAiAgentAnalysis.backend.analyse.http.JsonHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for embeddings from a local Ollama server (/api/embed).
 * Request: {"model": "bge-m3", "input": ["text 1", "text 2"]}
 * Answer:  {"embeddings": [[...], [...]]}
 */
public class OllamaEmbedder implements Embedder {

    private final JsonHttpClient http;
    private final String url;
    private final String model;
    private final int batchSize;

    public OllamaEmbedder(JsonHttpClient http, String baseUrl, String model, int batchSize) {
        this.http = http;
        this.url = baseUrl.replaceAll("/+$", "") + "/api/embed";
        this.model = model;
        this.batchSize = batchSize;
    }

    @Override
    public List<double[]> embed(List<String> texts) throws IOException, InterruptedException {
        List<double[]> vectors = new ArrayList<>();
        for (int start = 0; start < texts.size(); start += batchSize) {
            List<String> batch = texts.subList(start, Math.min(start + batchSize, texts.size()));
            System.out.printf("  Embedder %d-%d af %d%n", start + 1, start + batch.size(), texts.size());

            ObjectNode body = http.mapper().createObjectNode();
            body.put("model", model);
            ArrayNode input = body.putArray("input");
            batch.forEach(input::add);

            JsonNode embeddings = http.postJson(url, body).path("embeddings");
            if (!embeddings.isArray() || embeddings.size() != batch.size()) {
                throw new IOException("Uventet svar fra Ollama (forventede " + batch.size() + " vektorer). "
                        + "Er modellen hentet? Kør: ollama pull " + model);
            }
            for (JsonNode e : embeddings) {
                double[] v = new double[e.size()];
                for (int j = 0; j < v.length; j++) {
                    v[j] = e.get(j).asDouble();
                }
                vectors.add(v);
            }
        }
        return vectors;
    }
}
