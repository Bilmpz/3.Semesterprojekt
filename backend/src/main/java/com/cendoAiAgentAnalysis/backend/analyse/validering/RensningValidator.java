package com.cendoAiAgentAnalysis.backend.analyse.validering;

import java.util.regex.Pattern;

/** Trin 2: tjekker at der ikke er persondata tilbage efter rensning. Fejl gælder én samtale. */
public class RensningValidator implements StepValidator<String> {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern CPR = Pattern.compile("\\b\\d{6}-?\\d{4}\\b");
    // Danske telefonnumre: 8 cifre, evt. med +45 og mellemrum (fx "12 34 56 78").
    private static final Pattern TELEFON = Pattern.compile("(?<!\\d)(\\+45\\s?)?\\d{2}\\s?\\d{2}\\s?\\d{2}\\s?\\d{2}(?!\\d)");

    @Override
    public ValidationResult validate(String renset) {
        if (renset == null || renset.isBlank()) {
            return ValidationResult.fejl("tom_efter_rensning", "Rensningen fjernede al tekst");
        }
        if (EMAIL.matcher(renset).find()) {
            return ValidationResult.fejl("persondata_email", "Der er stadig en e-mailadresse");
        }
        // Telefon tjekkes før CPR, så "+4512345678" ikke fejlagtigt læses som et CPR-nummer.
        if (TELEFON.matcher(renset).find()) {
            return ValidationResult.fejl("persondata_telefon", "Der er stadig et telefonnummer");
        }
        if (CPR.matcher(renset).find()) {
            return ValidationResult.fejl("persondata_cpr", "Der er stadig et CPR-lignende nummer");
        }
        return ValidationResult.gyldig();
    }

    @Override
    public String trin() {
        return "Rensning";
    }
}
