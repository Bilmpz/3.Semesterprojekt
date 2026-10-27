package com.cendoAiAgentAnalysis.backend.analyse.model;

/**
 * The LLM's answer for one conversation.
 *
 * @param primary     the short primary reason; this is what gets embedded and clustered
 * @param explanation one sentence about the specific conversation; only shown next to the reason
 */
public record TransferReason(String primary, String explanation) {
}
