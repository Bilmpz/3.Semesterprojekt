package com.cendoAiAgentAnalysis.backend.analyse.extraction;

import com.cendoAiAgentAnalysis.backend.analyse.llm.ChatClient;
import com.cendoAiAgentAnalysis.backend.analyse.model.TransferReason;
import com.cendoAiAgentAnalysis.backend.analyse.util.JsonText;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Asks an LLM for the primary transfer reason (a few words, used for clustering) and one sentence
 * explaining the specific conversation (only shown in the results, so a person can read the details).
 *
 * Important design choice: the LLM gets NO list of categories. If it did, the LLM would be doing the
 * grouping and HDBSCAN would be pointless. The grouping must come from the data (Cendo's requirement).
 */
public class LlmReasonExtractor implements ReasonExtractor {

    /** Bump this when the prompt changes, so old cached answers are not reused. */
    public static final String PROMPT_VERSION = "v3";

    private static final String SYSTEM_PROMPT = """
            Du analyserer transskriptioner af telefonsamtaler mellem en AI-agent på et bilværksted og en kunde.
            Samtalen endte med, at kunden blev viderestillet til en menneskelig medarbejder.

            Giv to ting på dansk:
            1. "primary_reason": den primære årsag til viderestillingen som en kort betegnelse på 2-6 ord.
               Skriv emnet som en overskrift, ikke en sætning. Ordet "kunde" må ikke indgå.
               Nævn ikke navne, bilmærker, datoer eller andre detaljer, der kun gælder denne samtale,
               så samtaler med samme årsag får samme slags betegnelse.
               Gode eksempler: "Spørgsmål om garanti", "Ønske om specifik medarbejder", "Status på reparation",
               "Dårlig forbindelse", "For lang ventetid på værkstedstid".
               Dårlige eksempler: "Kunden ønskede at høre om garantien", "Kunde ønsker at tale med Jonas".
            2. "explanation": én sætning (højst 25 ord), der forklarer den konkrete situation i samtalen.

            Brug ikke engelske ord eller kategorinavne fra systemet.

            Svar KUN med JSON på formen: {"primary_reason": "...", "explanation": "..."}
            """;

    private final ChatClient chat;
    private final ObjectMapper mapper;

    public LlmReasonExtractor(ChatClient chat, ObjectMapper mapper) {
        this.chat = chat;
        this.mapper = mapper;
    }

    public String cacheKeyPart() {
        return chat.model() + "|" + PROMPT_VERSION;
    }

    @Override
    public TransferReason extractReason(String transcript) throws IOException, InterruptedException {
        String answer = chat.complete(SYSTEM_PROMPT, "Transskription:\n" + transcript);
        JsonNode json = JsonText.firstObject(answer, mapper);
        if (json != null && json.hasNonNull("primary_reason")) {
            return new TransferReason(ReasonCleaner.clean(json.get("primary_reason").asText()),
                    json.path("explanation").asText("").strip());
        }
        // The model ignored the JSON format: use its raw answer on one line for both
        String raw = answer.replace('\n', ' ').strip();
        // A cut-off or rambling answer must not become a very long "reason"
        String primary = raw.length() > 80 ? raw.substring(0, 80).strip() : raw;
        return new TransferReason(primary, raw);
    }
}