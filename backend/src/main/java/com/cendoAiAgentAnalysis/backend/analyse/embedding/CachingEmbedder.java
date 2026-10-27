package com.cendoAiAgentAnalysis.backend.analyse.embedding;

import com.cendoAiAgentAnalysis.backend.analyse.cache.JsonCache;
import com.cendoAiAgentAnalysis.backend.analyse.util.Hashing;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Decorator: only sends texts to the wrapped Embedder that have not been embedded before. */
public class CachingEmbedder implements Embedder {

    private final Embedder delegate;
    private final JsonCache cache;
    private final String model;
    private final ObjectMapper mapper;

    public CachingEmbedder(Embedder delegate, JsonCache cache, String model, ObjectMapper mapper) {
        this.delegate = delegate;
        this.cache = cache;
        this.model = model;
        this.mapper = mapper;
    }

    @Override
    public List<double[]> embed(List<String> texts) throws IOException, InterruptedException {
        List<String> missing = new ArrayList<>();
        for (String text : texts) {
            if (cache.get(key(text)) == null && !missing.contains(text)) {
                missing.add(text);
            }
        }
        System.out.printf("  %d tekster i cache, %d skal embeddes%n", texts.size() - missing.size(), missing.size());

        if (!missing.isEmpty()) {
            List<double[]> fresh = delegate.embed(missing);
            for (int i = 0; i < missing.size(); i++) {
                ArrayNode array = mapper.createArrayNode();
                for (double x : fresh.get(i)) {
                    array.add(x);
                }
                cache.put(key(missing.get(i)), array);
            }
            cache.save();
        }

        List<double[]> result = new ArrayList<>();
        for (String text : texts) {
            JsonNode array = cache.get(key(text));
            double[] v = new double[array.size()];
            for (int j = 0; j < v.length; j++) {
                v[j] = array.get(j).asDouble();
            }
            result.add(v);
        }
        return result;
    }

    private String key(String text) {
        return Hashing.sha256(model, text);
    }
}
