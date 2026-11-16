package com.cendoAiAgentAnalysis.backend.analyse.validering;

/** Resultatet af et tjek. Er det ikke ok, siger fejlKode hvad der gik galt. */
public record ValidationResult(boolean ok, String fejlKode, String besked) {

    public static ValidationResult gyldig() {
        return new ValidationResult(true, null, null);
    }

    public static ValidationResult fejl(String fejlKode, String besked) {
        return new ValidationResult(false, fejlKode, besked);
    }
}
