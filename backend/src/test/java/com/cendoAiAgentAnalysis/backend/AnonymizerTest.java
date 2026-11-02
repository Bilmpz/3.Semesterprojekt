// ID:          A4
// Hvad:        Tester at Anonymizer fjerner følsomme data.


// Sådan køres testen (fra mappen 3.Semesterprojekt, i PowerShell):
//   cd backend
//   $env:JAVA_HOME = (Get-ChildItem "C:\Program Files\Eclipse Adoptium\jdk-25*")[0].FullName
//   .\mvnw.cmd test "-Dtest=AnonymizerTest"
// Det virker, når der står "Tests run: 3, Failures: 0" og "BUILD SUCCESS".
// Kræver Java 25 (installeres med: winget install EclipseAdoptium.Temurin.25.JDK)


// -HUSK AT SLETTE- //
// TESTEN LAVET FULDT UD MED AI, har ingen ide om hvad der sker her. 
// -HUSK AT SLETTE- //

package com.cendoAiAgentAnalysis.backend;

import com.cendoAiAgentAnalysis.backend.analyse.data.Anonymizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnonymizerTest {

    // Test 1: En samtale med navn, tlf., nummerplade og e-mail skal blive renset
        // Test 1: En samtale med navn, tlf., nummerplade og e-mail skal blive renset
    @Test
    void fjernerFoelsomData() {
        String samtale = "Hej, jeg hedder Peter Hansen. Ring på 22 33 44 55 eller +45 12345678. "
                + "Bilen er AB 12 345, mail peter.h@gmail.com.";

        String renset = Anonymizer.anonymize(samtale);

        // Print før og efter, så man kan se det i terminalen
        System.out.println("FØR:   " + samtale);
        System.out.println("EFTER: " + renset);

        String forventet = "Hej, jeg hedder [NAVN]. Ring på [TLF] eller [TLF]. "
                + "Bilen er [REGNR], mail [EMAIL].";

        assertEquals(forventet, renset);
    }

    // Test 2: Almindelig tekst uden følsomme data skal være uændret
    @Test
    void roererIkkeAlmindeligTekst() {
        String samtale = "Jeg vil gerne booke et syn på torsdag.";

        assertEquals(samtale, Anonymizer.anonymize(samtale));
    }

    // Test 3: Hvis der ikke er nogen tekst (null), skal vi også få null tilbage
    @Test
    void nullGiverNull() {
        assertNull(Anonymizer.anonymize(null));
    }
}