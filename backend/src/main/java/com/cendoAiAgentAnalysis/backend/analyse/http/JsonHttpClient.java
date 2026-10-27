package com.cendoAiAgentAnalysis.backend.analyse.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * Sends JSON requests and parses JSON answers. Shared by all providers (Ollama, Hugging Face).
 * Retries when a service is busy (429 = rate limited, 503 = model loading, 502/504 = gateway problems).
 */
public class JsonHttpClient {

    private static final int MAX_ATTEMPTS = 5;
    private static final Set<Integer> RETRYABLE = Set.of(429, 502, 503, 504);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final String bearerToken;   // null = no Authorization header (Ollama)
    private final Duration requestTimeout;
    private final ObjectMapper mapper;

    public JsonHttpClient(String bearerToken, Duration requestTimeout, ObjectMapper mapper) {
        this.bearerToken = bearerToken;
        this.requestTimeout = requestTimeout;
        this.mapper = mapper;
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public JsonNode postJson(String url, JsonNode body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        HttpRequest request = builder.build();

        for (int attempt = 1; ; attempt++) {
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (ConnectException e) {
                throw new IOException("Kunne ikke forbinde til " + url
                        + ". Hvis I bruger Ollama: kører Ollama (lama-ikonet i proceslinjen)?", e);
            } catch (IOException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                waitBeforeRetry(attempt, "netværksfejl: " + e.getMessage());
                continue;
            }

            int status = response.statusCode();
            if (status == 200) {
                return mapper.readTree(response.body());
            }
            if (RETRYABLE.contains(status) && attempt < MAX_ATTEMPTS) {
                waitBeforeRetry(attempt, "HTTP " + status);
                continue;
            }
            throw new IOException("HTTP " + status + " fra " + url + "\n" + shorten(response.body()));
        }
    }

    private static void waitBeforeRetry(int attempt, String reason) throws InterruptedException {
        long waitMs = 5_000L * attempt;
        System.out.printf("  (%s - prøver igen om %d sek.)%n", reason, waitMs / 1000);
        Thread.sleep(waitMs);
    }

    private static String shorten(String s) {
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
