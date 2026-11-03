// ID:          A4
// Delsystem:   Analyse
// Hvad:        FjernFølsomData(): fjerner navne, telefonnumre, nummerplader før LLM og før data gemmes (designmål 1). Kan bygges sammen med TranscriptSanitizer.
// Se docs/Byggeplan_v0.6.docx
// ID:          A4
// Delsystem:   Analyse
// Hvad:        Fjerner følsomme data, før teksten sendes til LLM'en og før den gemmes (designmål 1).
package com.cendoAiAgentAnalysis.backend.analyse.data;

public class Anonymizer {

    // Metoden tager en samtale (tekst) og giver en "renset" version tilbage,
    // hvor følsomme ting er skiftet ud med fx [TLF] eller [EMAIL].
    public static String anonymize(String tekst) {

        // Hvis der ikke er nogen tekst, er der intet at rense
        if (tekst == null) {
            return null;
        }

        // Vi starter med den originale tekst og retter i den trin for trin
        String renset = tekst;

        // 1) E-mails
        // Finder fx "peter.h@gmail.com": noget tekst, så @, så et domæne med punktum
        renset = renset.replaceAll("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]+", "[EMAIL]");

        // 2) Nummerplader
        // Danske plader er 2 store bogstaver + 5 tal, fx "AB 12 345" eller "AB12345"
        // " ?" betyder: der må gerne være et mellemrum, men det behøver der ikke
        renset = renset.replaceAll("[A-ZÆØÅ]{2} ?[0-9]{2} ?[0-9]{3}", "[REGNR]");

        // 3) Telefonnumre
        // Danske numre er 8 cifre, fx "22 33 44 55" eller "22334455"
        // "(\\+45 ?)?" betyder: der må gerne stå +45 foran
        // Vi tager nummerplader FØR telefonnumre, ellers kunne tallene i pladen blive læst som et tlf.nr.
        renset = renset.replaceAll("(\\+45 ?)?[0-9]{2} ?[0-9]{2} ?[0-9]{2} ?[0-9]{2}", "[TLF]");

        // 4) Navne
        // Det er svært at finde alle navne, så vi fanger dem kun, når kunden siger
        // "jeg hedder ..." eller "mit navn er ...". Fornavn + evt. efternavn bliver til [NAVN].
        // "$1" betyder: behold de første ord ("jeg hedder"), og skift kun selve navnet ud
        renset = renset.replaceAll(
                "(jeg hedder|Jeg hedder|mit navn er|Mit navn er) [A-ZÆØÅ][a-zæøå]+( [A-ZÆØÅ][a-zæøå]+)?",
                "$1 [NAVN]");

        // Giv den rensede tekst tilbage
        return renset;
    }
}