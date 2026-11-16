package com.cendoAiAgentAnalysis.backend.analyse.validering;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Trin 7: tjekker navnet på ét cluster. Fejl gælder kun det cluster:
 * prøv igen, eller giv det et standardnavn som "Cluster 3".
 * tjekAlleNavne(...) kan bruges til sidst for at finde manglende navne og dubletter.
 */
public class NavngivningValidator implements StepValidator<String> {

    private final int maxOrd;

    public NavngivningValidator() {
        this(8);
    }

    public NavngivningValidator(int maxOrd) {
        this.maxOrd = maxOrd;
    }

    @Override
    public ValidationResult validate(String navn) {
        if (navn == null || navn.isBlank()) {
            return ValidationResult.fejl("mangler_navn", "LLM'en gav intet navn");
        }
        String tekst = navn.strip();
        if (tekst.startsWith("{")) {
            return ValidationResult.fejl("ugyldig_json", "Svaret er rå JSON og ikke et navn");
        }
        int antalOrd = tekst.split("\\s+").length;
        if (antalOrd > maxOrd) {
            return ValidationResult.fejl("for_langt_navn", antalOrd + " ord, højst " + maxOrd);
        }
        return ValidationResult.gyldig();
    }

    /** Tjekker alle navne samlet (cluster-id → navn): manglende navne og dubletter. */
    public static ValidationResult tjekAlleNavne(Map<Integer, String> navne, Set<Integer> forventedeClusters) {
        if (navne == null || navne.isEmpty()) {
            return ValidationResult.fejl("ingen_navne", "Ingen clusters fik et navn");
        }
        Set<String> sete = new HashSet<>();
        for (Integer id : forventedeClusters) {
            String navn = navne.get(id);
            if (navn == null || navn.isBlank()) {
                return ValidationResult.fejl("mangler_navn", "Cluster " + id + " har intet navn");
            }
            if (!sete.add(navn.strip().toLowerCase())) {
                return ValidationResult.fejl("dublet_navn", "Navnet \"" + navn + "\" bruges flere gange");
            }
        }
        return ValidationResult.gyldig();
    }

    @Override
    public String trin() {
        return "Navngivning";
    }
}
