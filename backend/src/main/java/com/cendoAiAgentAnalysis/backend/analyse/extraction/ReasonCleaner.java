package com.cendoAiAgentAnalysis.backend.analyse.extraction;

import java.util.regex.Pattern;

/**
 * Removes the stock opening some models put in front of the primary reason, e.g.
 * "Kunden ønskede at tale med en mekaniker" -> "Tale med en mekaniker".
 *
 * Without this, every reason shares the same first words, which pulls unrelated reasons
 * close together in the embedding space.
 */
public final class ReasonCleaner {

    private static final Pattern OPENING = Pattern.compile(
            "^(kunden?s?\\s+(ønske|ønsker|ønskede|ville|vil|bad om|beder om|havde brug for|har brug for|"
                    + "behøvede|behøver|foretrak|foretrækker|krævede|kræver|spurgte om|ringede om)"
                    + "(\\s+(at|om))?\\s+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private ReasonCleaner() {
    }

    public static String clean(String reason) {
        String s = reason.strip();
        var m = OPENING.matcher(s);
        if (!m.find()) {
            return s;
        }
        String verb = m.group(2).toLowerCase();
        String rest = s.substring(m.end()).strip();
        if (rest.isEmpty()) {
            return s;
        }
        // "Kundens ønske om X" -> "Ønske om X" keeps the meaning; for the others the rest stands alone
        if (verb.equals("ønske") && s.toLowerCase().startsWith("kundens")) {
            rest = "Ønske om " + rest;
        }
        return Character.toUpperCase(rest.charAt(0)) + rest.substring(1);
    }
}
