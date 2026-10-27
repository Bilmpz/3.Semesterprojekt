package com.cendoAiAgentAnalysis.backend.analyse.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Helpers for reading JSON out of free-text LLM answers (which sometimes wrap it in ``` or extra words). */
public final class JsonText {

    private JsonText() {
    }

    /** Returns the first {...} object in the text, or null if there is none or it cannot be parsed. */
    public static JsonNode firstObject(String text, ObjectMapper mapper) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return mapper.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }
}
