package com.cendoAiAgentAnalysis.backend.analyse.llm;

import java.io.IOException;

/** A language model that answers a system prompt + user message. Implemented per provider. */
public interface ChatClient {

    String complete(String systemPrompt, String userMessage) throws IOException, InterruptedException;

    /** Model name, used in cache keys so answers from different models are never mixed up. */
    String model();
}
