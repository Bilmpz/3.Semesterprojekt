package com.cendoAiAgentAnalysis.backend.analyse.llm;

import com.cendoAiAgentAnalysis.backend.analyse.http.JsonHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/** Adapter for Hugging Face's OpenAI-compatible chat endpoint (Inference Providers, requires credits). */
public class HuggingFaceChatClient implements ChatClient {

    private final JsonHttpClient http;
    private final String url;
    private final String model;

    public HuggingFaceChatClient(JsonHttpClient http, String url, String model) {
        this.http = http;
        this.url = url;
        this.model = model;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public String complete(String systemPrompt, String userMessage) throws IOException, InterruptedException {
        ObjectNode body = http.mapper().createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.1);   // low temperature = consistent phrasing across conversations
        body.put("max_tokens", 300);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userMessage);

        JsonNode response = http.postJson(url, body);
        String content = response.path("choices").path(0).path("message").path("content").asText("");
        return LlmText.removeThinking(content).strip();
    }
}
