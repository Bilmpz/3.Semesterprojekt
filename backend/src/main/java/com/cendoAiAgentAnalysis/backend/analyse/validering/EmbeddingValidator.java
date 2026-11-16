package com.cendoAiAgentAnalysis.backend.analyse.validering;

/** Trin 4: tjekker en embedding-vektor. Fejl gælder én samtale. */
public class EmbeddingValidator implements StepValidator<double[]> {

    private final int forventetDimension;

    /** forventetDimension = 0 betyder at alle længder accepteres. */
    public EmbeddingValidator(int forventetDimension) {
        this.forventetDimension = forventetDimension;
    }

    @Override
    public ValidationResult validate(double[] vektor) {
        if (vektor == null || vektor.length == 0) {
            return ValidationResult.fejl("tom_vektor", "Embedding mangler");
        }
        if (forventetDimension > 0 && vektor.length != forventetDimension) {
            return ValidationResult.fejl("forkert_dimension",
                    vektor.length + " dimensioner, forventet " + forventetDimension);
        }
        boolean altNul = true;
        for (double v : vektor) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return ValidationResult.fejl("ugyldig_vaerdi", "Vektoren indeholder NaN eller uendelig");
            }
            if (v != 0.0) {
                altNul = false;
            }
        }
        if (altNul) {
            return ValidationResult.fejl("nulvektor", "Alle værdier er 0");
        }
        return ValidationResult.gyldig();
    }

    @Override
    public String trin() {
        return "Embedding";
    }
}
