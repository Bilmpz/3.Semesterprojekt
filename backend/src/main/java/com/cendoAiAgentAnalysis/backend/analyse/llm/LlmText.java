package com.cendoAiAgentAnalysis.backend.analyse.llm;

/** Clean-up shared by all chat clients. */
final class LlmText {

    private LlmText() {
    }

    /** Some models (e.g. Qwen3) prefix their answer with <think>...</think>; we only want the answer. */
    static String removeThinking(String text) {
        int end = text.lastIndexOf("</think>");
        return end >= 0 ? text.substring(end + "</think>".length()) : text;
    }
}
