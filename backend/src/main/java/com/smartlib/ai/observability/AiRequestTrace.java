package com.smartlib.ai.observability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured AI request telemetry trace.
 *
 * PRIVACY GUARD:
 * Does NOT contain prompts, model outputs, memory contents, raw JWTs,
 * passwords, API keys, or member identifiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestTrace {

    private String requestId;
    private String provider;
    private String model;
    private boolean fallbackUsed;
    private boolean webGroundingUsed;
    private boolean memoryUsed;
    private boolean rerankingUsed;
    private int toolCount;
    @Builder.Default
    private List<String> toolNames = new ArrayList<>();
    private long qdrantLatencyMs;
    private long mysqlLatencyMs;
    private long providerLatencyMs;
    private long totalLatencyMs;
    private boolean success;
    private AiErrorCategory errorCategory;
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Produces a privacy-safe single-line structured log string.
     */
    public String toStructuredLog() {
        return String.format(
                "AI_REQUEST_COMPLETED requestId=%s provider=%s model=%s fallback=%s webGrounding=%s memoryUsed=%s reranking=%s toolCount=%d totalLatencyMs=%d success=%s errorCategory=%s",
                requestId != null ? requestId : "UNKNOWN",
                provider != null ? provider : "unknown",
                model != null ? model : "unknown",
                fallbackUsed,
                webGroundingUsed,
                memoryUsed,
                rerankingUsed,
                toolCount,
                totalLatencyMs,
                success,
                errorCategory != null ? errorCategory.name() : AiErrorCategory.NONE.name()
        );
    }
}
