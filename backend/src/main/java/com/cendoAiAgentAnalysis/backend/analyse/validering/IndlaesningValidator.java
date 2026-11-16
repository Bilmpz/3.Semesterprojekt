package com.cendoAiAgentAnalysis.backend.analyse.validering;

/** Trin 1: tjekker en indlæst transskription. Fejl her gælder kun én samtale. */
public class IndlaesningValidator implements StepValidator<String> {

    private final int minTegn;

    public IndlaesningValidator() {
        this(20);
    }

    public IndlaesningValidator(int minTegn) {
        this.minTegn = minTegn;
    }

    @Override
    public ValidationResult validate(String transskription) {
        if (transskription == null || transskription.isBlank()) {
            return ValidationResult.fejl("tom_samtale", "Transskriptionen er tom");
        }
        if (transskription.strip().length() < minTegn) {
            return ValidationResult.fejl("for_kort", "Under " + minTegn + " tegn");
        }
        return ValidationResult.gyldig();
    }

    @Override
    public String trin() {
        return "Indlæsning";
    }
}
