package com.cendoAiAgentAnalysis.backend.analyse.llm;

import com.cendoAiAgentAnalysis.backend.analyse.http.JsonHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Adapter for a local Ollama server (free, runs on your own computer).
 * Uses Ollama's native /api/chat endpoint because it lets us set two important options:
 *  - format=json: forces the model to answer with valid JSON (small models often forget otherwise)
 *  - num_ctx: the context window. Ollama's default is small and silently cuts off long prompts.
 * Some new models (e.g. DFM Mimir, built on Gemma 4) run fine in the terminal but fail when
 * format=json is set. If a call fails, we try once more without it and keep that for the rest of the run.
 * The prompts ask for JSON anyway, and the JSON object is read out of the answer text.
 */
public class OllamaChatClient implements ChatClient {

    private static final int CONTEXT_TOKENS = 8192;
    // Upper limit on the answer length. Small models (e.g. DFM Mimir) sometimes get stuck repeating
    // themselves and never stop, so the request times out. Our answers are short, so 400 tokens is plenty.
    private static final int MAX_ANSWER_TOKENS = 400;

    private final JsonHttpClient http;
    private final String url;
    private final String model;
    private boolean useJsonFormat = true;

    public OllamaChatClient(JsonHttpClient http, String baseUrl, String model) {
        this.http = http;
        this.url = baseUrl.replaceAll("/+$", "") + "/api/chat";
        this.model = model;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public String complete(String systemPrompt, String userMessage) throws IOException, InterruptedException {
        if (useJsonFormat) {
            try {
                return send(systemPrompt, userMessage, true);
            } catch (IOException e) {
                System.out.println("  " + model + " fejlede med format=json (" + firstLine(e.getMessage())
                        + "). Prøver igen uden.");
                useJsonFormat = false;
            }
        }
        return send(systemPrompt, userMessage, false);
    }

    private String send(String systemPrompt, String userMessage, boolean jsonFormat)
            throws IOException, InterruptedException {
        ObjectNode body = http.mapper().createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        if (jsonFormat) {
            body.put("format", "json");
        }
        ObjectNode options = body.putObject("options");
        options.put("temperature", 0.1);
        options.put("num_ctx", CONTEXT_TOKENS);
        options.put("num_predict", MAX_ANSWER_TOKENS);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userMessage);

        JsonNode response = http.postJson(url, body);
        String content = response.path("message").path("content").asText("");
        return LlmText.removeThinking(content).strip();
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "ukendt fejl";
        }
        int newline = message.indexOf('\n');
        return newline >= 0 ? message.substring(0, newline) : message;
    }
}