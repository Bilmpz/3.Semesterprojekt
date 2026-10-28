package com.cendoAiAgentAnalysis.backend.analyse.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A key -> JSON value store saved as one file. Used so we never pay for (or wait for)
 * the same API call twice, e.g. when re-running the clustering with new parameters.
 */
public class JsonCache {

    private final Path file;
    private final ObjectMapper mapper;
    private final ObjectNode entries;

    public JsonCache(Path file, ObjectMapper mapper) throws IOException {
        this.file = file;
        this.mapper = mapper;
        if (Files.exists(file)) {
            this.entries = (ObjectNode) mapper.readTree(file.toFile());
        } else {
            this.entries = mapper.createObjectNode();
        }
    }

    public JsonNode get(String key) {
        return entries.get(key);
    }

    public void put(String key, JsonNode value) {
        entries.set(key, value);
    }

    public int size() {
        return entries.size();
    }

    public void save() throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        mapper.writeValue(file.toFile(), entries);
    }
}
