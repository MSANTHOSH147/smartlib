package com.smartlib.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AiMetricsService {

    private final MeterRegistry meterRegistry;

    @Autowired
    public AiMetricsService(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    public void recordRequest(AiRequestTrace trace) {
        if (trace == null) return;

        String provider = sanitizeTag(trace.getProvider(), "unknown");
        String model = sanitizeTag(trace.getModel(), "unknown");
        String success = String.valueOf(trace.isSuccess());
        String errorCategory = trace.getErrorCategory() != null ? trace.getErrorCategory().name() : AiErrorCategory.NONE.name();

        Counter.builder("smartlib_ai_requests_total")
                .description("Total number of AI requests processed")
                .tag("provider", provider)
                .tag("model", model)
                .tag("success", success)
                .tag("error_category", errorCategory)
                .register(meterRegistry)
                .increment();

        if (!trace.isSuccess()) {
            Counter.builder("smartlib_ai_request_failures_total")
                    .description("Total number of failed AI requests")
                    .tag("provider", provider)
                    .tag("error_category", errorCategory)
                    .register(meterRegistry)
                    .increment();
        }

        Timer.builder("smartlib_ai_request_duration")
                .description("Execution duration of AI chat requests")
                .tag("provider", provider)
                .tag("success", success)
                .register(meterRegistry)
                .record(trace.getTotalLatencyMs(), TimeUnit.MILLISECONDS);

        if (trace.isFallbackUsed()) {
            recordFallback(provider, "groq");
        }
        if (trace.isWebGroundingUsed()) {
            recordWebGrounding();
        }
        if (trace.isMemoryUsed()) {
            recordMemoryUsage();
        }
        if (trace.isRerankingUsed()) {
            recordReranking();
        }

        if (trace.getToolNames() != null) {
            for (String tool : trace.getToolNames()) {
                recordToolCall(tool);
            }
        }

        if (trace.getProviderLatencyMs() > 0) {
            Timer.builder("smartlib_ai_provider_duration")
                    .description("Duration of LLM provider execution")
                    .tag("provider", provider)
                    .register(meterRegistry)
                    .record(trace.getProviderLatencyMs(), TimeUnit.MILLISECONDS);
        }

        if (trace.getQdrantLatencyMs() > 0) {
            recordQdrantDuration(trace.getQdrantLatencyMs());
        }

        if (trace.getMysqlLatencyMs() > 0) {
            recordMysqlDuration(trace.getMysqlLatencyMs());
        }
    }

    public void recordProviderRequest(String provider, String model) {
        Counter.builder("smartlib_ai_provider_requests_total")
                .description("Total requests made to LLM providers")
                .tag("provider", sanitizeTag(provider, "unknown"))
                .tag("model", sanitizeTag(model, "unknown"))
                .register(meterRegistry)
                .increment();
    }

    public void recordFallback(String primaryProvider, String fallbackProvider) {
        Counter.builder("smartlib_ai_provider_fallback_total")
                .description("Total provider fallbacks triggered")
                .tag("primary_provider", sanitizeTag(primaryProvider, "gemini"))
                .tag("fallback_provider", sanitizeTag(fallbackProvider, "groq"))
                .register(meterRegistry)
                .increment();
    }

    public void recordToolCall(String toolName) {
        Counter.builder("smartlib_ai_tool_calls_total")
                .description("Total tool function executions")
                .tag("tool", sanitizeTag(toolName, "unknown"))
                .register(meterRegistry)
                .increment();
    }

    public void recordWebGrounding() {
        Counter.builder("smartlib_ai_web_grounding_total")
                .description("Total AI requests utilizing web grounding")
                .register(meterRegistry)
                .increment();
    }

    public void recordMemoryUsage() {
        Counter.builder("smartlib_ai_memory_usage_total")
                .description("Total AI requests referencing user memory")
                .register(meterRegistry)
                .increment();
    }

    public void recordReranking() {
        Counter.builder("smartlib_ai_reranking_total")
                .description("Total hybrid searches executing reranking")
                .register(meterRegistry)
                .increment();
    }

    public void recordQdrantDuration(long durationMs) {
        Timer.builder("smartlib_ai_qdrant_duration")
                .description("Duration of Qdrant vector search operations")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordMysqlDuration(long durationMs) {
        Timer.builder("smartlib_ai_mysql_duration")
                .description("Duration of MySQL search operations")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    private String sanitizeTag(String tagValue, String defaultValue) {
        if (tagValue == null || tagValue.trim().isEmpty()) {
            return defaultValue;
        }
        return tagValue.trim().toLowerCase();
    }
}
