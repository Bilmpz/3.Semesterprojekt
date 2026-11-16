package com.cendoAiAgentAnalysis.backend.analyse.validering;

/** Trin 5: tjekker resultatet af dimensionsreduktion (UMAP). Fejl stopper hele kørslen. */
public class ReduktionValidator implements StepValidator<double[][]> {

    private final int forventetAntalRaekker;
    private final int forventetDimension;

    public ReduktionValidator(int forventetAntalRaekker, int forventetDimension) {
        this.forventetAntalRaekker = forventetAntalRaekker;
        this.forventetDimension = forventetDimension;
    }

    @Override
    public ValidationResult validate(double[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            return ValidationResult.fejl("tomt_resultat", "Reduktionen gav ingen rækker");
        }
        if (matrix.length != forventetAntalRaekker) {
            return ValidationResult.fejl("forkert_antal_raekker",
                    matrix.length + " rækker, forventet " + forventetAntalRaekker);
        }
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] == null || matrix[i].length != forventetDimension) {
                return ValidationResult.fejl("forkert_dimension",
                        "Række " + i + " har ikke " + forventetDimension + " dimensioner");
            }
            for (double v : matrix[i]) {
                if (Double.isNaN(v) || Double.isInfinite(v)) {
                    return ValidationResult.fejl("ugyldig_vaerdi", "Række " + i + " indeholder NaN eller uendelig");
                }
            }
        }
        return ValidationResult.gyldig();
    }

    @Override
    public String trin() {
        return "Dimensionsreduktion";
    }
}
