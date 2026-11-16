package com.cendoAiAgentAnalysis.backend.analyse.validering;

/**
 * Fælles interface for alle filtre i pipelinen.
 * T er den type, et trin sender videre (fx String, double[] eller int[]).
 */
public interface StepValidator<T> {

    ValidationResult validate(T output);

    /** Navnet på trinnet, så fejlbeskeder viser hvor det gik galt. */
    String trin();

    /** Returnerer output uændret, hvis det er gyldigt. Ellers kastes StepFailedException. */
    default T kraevGyldig(T output) {
        ValidationResult r = validate(output);
        if (!r.ok()) {
            throw new StepFailedException(trin(), r.fejlKode(), r.besked());
        }
        return output;
    }
}
