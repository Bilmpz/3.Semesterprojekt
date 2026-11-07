// ID:          W3
// Delsystem:   Web-API
// Hvad:        Fælles fejlsvar for alle controllere: { "fejl": "..." } med den rigtige statuskode.
// Se docs/Byggeplan_v0.6.docx
package com.cendoAiAgentAnalysis.backend.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// @RestControllerAdvice gælder for ALLE controllere (Overblik, Cluster, Samtale).
// Når en controller kaster en fejl, fanger Spring den her i stedet for at sende sin egen fejlside.
// Så svarer API'et altid på samme måde, og frontend kan vise beskeden.
@RestControllerAdvice
public class FejlHaandtering {

    // Fejl, som controllerne selv kaster med en statuskode, fx 404 (ukendt id) eller 400 (days < 1)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> statusFejl(ResponseStatusException e) {
        String besked = e.getReason() != null ? e.getReason() : "Der skete en fejl";
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("fejl", besked));
    }

    // En parameter har forkert type, fx /api/overblik?days=abc -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> forkertParameter(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("fejl", "Ugyldig værdi for " + e.getName()));
    }

    // Alt andet, vi ikke havde forudset -> 500.
    // Vi viser ikke selve fejlen til brugeren, fordi den kan indeholde data (designmål 1).
    // Den skrives i stedet i konsollen, så vi kan finde den.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> uventetFejl(Exception e) {
        System.err.println("Uventet fejl i API'et: " + e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("fejl", "Der skete en uventet fejl"));
    }
}
