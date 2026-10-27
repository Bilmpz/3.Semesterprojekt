package com.cendoAiAgentAnalysis.backend.analyse.naming;

import com.cendoAiAgentAnalysis.backend.analyse.llm.ChatClient;
import com.cendoAiAgentAnalysis.backend.analyse.model.ClusterName;
import com.cendoAiAgentAnalysis.backend.analyse.util.JsonText;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/** Asks an LLM to name a cluster from a sample of its texts. */
public class LlmClusterNamer implements ClusterNamer {

    private static final int MAX_EXAMPLES = 12;
    private static final int MAX_CHARS_PER_EXAMPLE = 700;

    private static final String SYSTEM_PROMPT = """
            Du får eksempler fra én gruppe af telefonsamtaler, hvor en AI-agent på et bilværksted
            viderestillede kunden til en menneskelig medarbejder. Eksemplerne er grupperet automatisk,
            fordi de ligner hinanden.

            Find det fælles tema for viderestillingerne og giv gruppen:
            - et kort navn på dansk (højst 6 ord)
            - en beskrivelse på én sætning af, hvorfor kunderne blev viderestillet

            Svar KUN med JSON på formen: {"name": "...", "description": "..."}
            """;

    private final ChatClient chat;
    private final ObjectMapper mapper;

    public LlmClusterNamer(ChatClient chat, ObjectMapper mapper) {
        this.chat = chat;
        this.mapper = mapper;
    }

    @Override
    public ClusterName name(List<String> examples) throws IOException, InterruptedException {
        StringBuilder message = new StringBuilder("Eksempler fra gruppen:\n");
        int count = Math.min(MAX_EXAMPLES, examples.size());
        for (int i = 0; i < count; i++) {
            String text = examples.get(i);
            if (text.length() > MAX_CHARS_PER_EXAMPLE) {
                text = text.substring(0, MAX_CHARS_PER_EXAMPLE) + " [...]";
            }
            message.append("\n--- Eksempel ").append(i + 1).append(" ---\n").append(text).append('\n');
        }

        String answer = chat.complete(SYSTEM_PROMPT, message.toString());
        JsonNode json = JsonText.firstObject(answer, mapper);
        if (json != null && json.hasNonNull("name")) {
            return new ClusterName(json.get("name").asText().strip(), json.path("description").asText("").strip());
        }
        return new ClusterName(answer.replace('\n', ' ').strip(), "");
    }
}
