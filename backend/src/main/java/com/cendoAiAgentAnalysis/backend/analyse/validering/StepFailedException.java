package com.cendoAiAgentAnalysis.backend.analyse.validering;

/** Kastes når et trins output ikke er gyldigt. */
public class StepFailedException extends RuntimeException {

    private final String trin;
    private final String fejlKode;

    public StepFailedException(String trin, String fejlKode, String besked) {
        super(trin + ": " + fejlKode + " (" + besked + ")");
        this.trin = trin;
        this.fejlKode = fejlKode;
    }

    public String trin() {
        return trin;
    }

    public String fejlKode() {
        return fejlKode;
    }
}
