package com.smartlib.ai.evaluation;

import lombok.*;

import java.util.Map;

/**
 * Report containing factual, measured benchmark metrics from evaluation runs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiEvaluationReport {

    private int totalCases;
    private int evaluatedCases;
    private double toolSelectionAccuracy;
    private double expectedToolPresenceRate;
    private int forbiddenToolUsageCount;
    private double sourceTypeCorrectnessRate;
    private int securityViolationCount;
    private double webGroundingUsageCorrectnessRate;
    private double fallbackCorrectnessRate;
    private Map<String, Integer> categoryCounts;

    public String printSummary() {
        return String.format(
                """
                ==================================================
                SMARTLIB AI EVALUATION REPORT
                ==================================================
                Evaluation cases:                      %d
                Tool selection accuracy:               %.1f%%
                Expected tool presence:                %.1f%%
                Source classification accuracy:        %.1f%%
                Security violations:                   %d
                Forbidden tool usages:                 %d
                Web grounding correctness:             %.1f%%
                Fallback correctness:                  %.1f%%
                ==================================================
                """,
                totalCases,
                toolSelectionAccuracy * 100.0,
                expectedToolPresenceRate * 100.0,
                sourceTypeCorrectnessRate * 100.0,
                securityViolationCount,
                forbiddenToolUsageCount,
                webGroundingUsageCorrectnessRate * 100.0,
                fallbackCorrectnessRate * 100.0
        );
    }
}
