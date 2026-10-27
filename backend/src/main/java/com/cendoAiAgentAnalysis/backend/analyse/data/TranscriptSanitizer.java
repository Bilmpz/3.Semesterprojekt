package com.cendoAiAgentAnalysis.backend.analyse.data;

import java.util.regex.Pattern;

/**
 * Removes information from a transcript that would reveal the answer.
 *
 * In the synthetic dataset the agent's tool call contains the ground-truth label, e.g.
 *   TOOL: transfer({reason: "price_or_invoice"})
 * If that reached the LLM or the embedding model, the clusters would simply copy the label
 * and the evaluation would be meaningless. We keep the fact that a transfer happened but drop its arguments.
 */
public final class TranscriptSanitizer {

    private static final Pattern TRANSFER_ARGUMENTS = Pattern.compile("transfer\\(\\{[^}]*}\\)");

    private TranscriptSanitizer() {
    }

    public static String sanitize(String transcript) {
        return TRANSFER_ARGUMENTS.matcher(transcript).replaceAll("transfer()");
    }
}
