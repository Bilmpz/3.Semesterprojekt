package com.cendoAiAgentAnalysis.backend.analyse.model;

/**
 * One transferred conversation from the dataset.
 *
 * @param id         conversation_id, e.g. SYN-0042 (used to join with the labels during evaluation)
 * @param split      train / validation / test
 * @param transcript the transcript with the transfer reason removed (see TranscriptSanitizer)
 */
public record Conversation(String id, String split, String transcript) {
}
