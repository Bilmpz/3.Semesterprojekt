package com.cendoAiAgentAnalysis.backend.analyse.validering;

/** Trin 3: tjekker LLM'ens årsagsbetegnelse (primary_reason). Fejl gælder én samtale. */
public class AarsagValidator implements StepValidator<String> {

    private final int minOrd;
    private final int maxOrd;

    public AarsagValidator() {
        this(2, 6);
    }

    public AarsagValidator(int minOrd, int maxOrd) {
        this.minOrd = minOrd;
        this.maxOrd = maxOrd;
    }

    @Override
    public ValidationResult validate(String aarsag) {
        if (aarsag == null || aarsag.isBlank()) {
            return ValidationResult.fejl("tom_aarsag", "LLM'en gav ingen årsag");
        }
        String tekst = aarsag.strip();
        if (tekst.startsWith("{") || tekst.contains("primary_reason")) {
            return ValidationResult.fejl("ugyldig_json", "Svaret er rå JSON og ikke en betegnelse");
        }
        int antalOrd = tekst.split("\\s+").length;
        if (antalOrd < minOrd || antalOrd > maxOrd) {
            return ValidationResult.fejl("forkert_laengde",
                    antalOrd + " ord, forventet " + minOrd + "-" + maxOrd);
        }
        if (tekst.toLowerCase().contains("kunde")) {
            return ValidationResult.fejl("indeholder_kunde", "Betegnelsen må ikke indeholde ordet \"kunde\"");
        }
        return ValidationResult.gyldig();
    }

    @Override
    public String trin() {
        return "Årsagsudtrækning";
    }
}
