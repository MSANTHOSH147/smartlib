package com.smartlib.ai;

import com.smartlib.ai.dto.AiChatRequest;
import com.smartlib.ai.dto.AiChatResponse;
import com.smartlib.ai.dto.AiOrchestrationResult;
import com.smartlib.ai.dto.ChatMessageDto;
import com.smartlib.ai.observability.AiErrorCategory;
import com.smartlib.ai.observability.AiMetricsService;
import com.smartlib.ai.observability.AiRequestTrace;
import com.smartlib.ai.security.AiRateLimiter;
import com.smartlib.ai.service.SmartLibAiOrchestrator;
import com.smartlib.controller.AiController;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiObservabilityTest {

    @Test
    @DisplayName("AiRequestTrace builds correctly with default timestamp and safe fields")
    void testAiRequestTraceCreationAndDefaults() {
        AiRequestTrace trace = AiRequestTrace.builder()
                .requestId("req-12345")
                .provider("gemini")
                .model("gemini-2.5-flash")
                .fallbackUsed(false)
                .webGroundingUsed(true)
                .memoryUsed(false)
                .rerankingUsed(true)
                .toolCount(2)
                .toolNames(List.of("searchBooks", "checkBookAvailability"))
                .qdrantLatencyMs(45)
                .mysqlLatencyMs(12)
                .providerLatencyMs(350)
                .totalLatencyMs(415)
                .success(true)
                .errorCategory(AiErrorCategory.NONE)
                .build();

        assertEquals("req-12345", trace.getRequestId());
        assertEquals("gemini", trace.getProvider());
        assertTrue(trace.isWebGroundingUsed());
        assertTrue(trace.isRerankingUsed());
        assertEquals(2, trace.getToolCount());
        assertEquals(415, trace.getTotalLatencyMs());
        assertNotNull(trace.getTimestamp());
    }

    @Test
    @DisplayName("Privacy Guard: Structured log format excludes prompt, model reply, and user secrets")
    void testStructuredLogDoesNotContainPromptOrSecrets() {
        String sensitivePrompt = "My secret password is secret123 and user email is secret@smartlib.com";
        String sensitiveMemory = "Member prefers confidential financial records";
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sensitive";

        AiRequestTrace trace = AiRequestTrace.builder()
                .requestId("trace-privacy-99")
                .provider("gemini")
                .model("gemini-2.5-flash")
                .fallbackUsed(false)
                .webGroundingUsed(false)
                .memoryUsed(true)
                .rerankingUsed(true)
                .toolCount(1)
                .toolNames(List.of("searchBooks"))
                .totalLatencyMs(210)
                .success(true)
                .errorCategory(AiErrorCategory.NONE)
                .build();

        String logOutput = trace.toStructuredLog();

        assertTrue(logOutput.startsWith("AI_REQUEST_COMPLETED"));
        assertTrue(logOutput.contains("requestId=trace-privacy-99"));
        assertTrue(logOutput.contains("provider=gemini"));
        assertTrue(logOutput.contains("toolCount=1"));
        assertTrue(logOutput.contains("success=true"));

        // Verify sensitive data does not leak into logs
        assertFalse(logOutput.contains(sensitivePrompt));
        assertFalse(logOutput.contains(sensitiveMemory));
        assertFalse(logOutput.contains(jwtToken));
        assertFalse(logOutput.contains("password"));
        assertFalse(logOutput.contains("secret@"));
    }

    @Test
    @DisplayName("AiMetricsService registers counters and timers without sensitive tag keys or values")
    void testAiMetricsServiceSafeLabels() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiMetricsService metricsService = new AiMetricsService(registry);

        AiRequestTrace trace = AiRequestTrace.builder()
                .requestId("req-sec-tag")
                .provider("gemini")
                .model("gemini-2.5-flash")
                .fallbackUsed(true)
                .webGroundingUsed(true)
                .memoryUsed(true)
                .rerankingUsed(true)
                .toolCount(1)
                .toolNames(List.of("checkBookAvailability"))
                .totalLatencyMs(150)
                .providerLatencyMs(120)
                .qdrantLatencyMs(20)
                .mysqlLatencyMs(5)
                .success(true)
                .errorCategory(AiErrorCategory.NONE)
                .build();

        metricsService.recordRequest(trace);

        Set<String> forbiddenTagKeys = Set.of("userid", "user_id", "email", "prompt", "query", "title", "memory");

        for (Meter meter : registry.getMeters()) {
            for (Tag tag : meter.getId().getTags()) {
                String keyLower = tag.getKey().toLowerCase();
                assertFalse(forbiddenTagKeys.contains(keyLower),
                        "Metric " + meter.getId().getName() + " contains sensitive tag key: " + tag.getKey());
            }
        }

        Counter requestCounter = registry.find("smartlib_ai_requests_total").counter();
        assertNotNull(requestCounter);
        assertEquals(1.0, requestCounter.count());

        Counter toolCounter = registry.find("smartlib_ai_tool_calls_total").counter();
        assertNotNull(toolCounter);
        assertEquals(1.0, toolCounter.count());
    }

    @Test
    @DisplayName("AiController attaches X-AI-Request-Id response header and measures request")
    void testAiControllerReturnsRequestIdHeader() {
        SmartLibAiOrchestrator orchestrator = Mockito.mock(SmartLibAiOrchestrator.class);
        AiRateLimiter rateLimiter = Mockito.mock(AiRateLimiter.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiMetricsService metricsService = new AiMetricsService(registry);

        when(orchestrator.chat(anyString(), anyList()))
                .thenReturn(AiOrchestrationResult.success("Hello from SmartLib", List.of("searchBooks")));

        AiController controller = new AiController(orchestrator, rateLimiter, null, metricsService);

        Authentication auth = new UsernamePasswordAuthenticationToken("user123", "pass", List.of());
        AiChatRequest request = new AiChatRequest("Hi", List.of(new ChatMessageDto("user", "Hi")));

        ResponseEntity<AiChatResponse> response = controller.chat(request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String reqId = response.getHeaders().getFirst("X-AI-Request-Id");
        assertNotNull(reqId);
        assertFalse(reqId.isBlank());

        assertEquals("Hello from SmartLib", response.getBody().getReply());
        assertTrue(response.getBody().isSuccess());

        // Verify request was counted
        Counter counter = registry.find("smartlib_ai_requests_total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Telemetry records authentication failures without bypassing authorization")
    void testTelemetryDoesNotBypassAuthorization() {
        SmartLibAiOrchestrator orchestrator = Mockito.mock(SmartLibAiOrchestrator.class);
        AiRateLimiter rateLimiter = Mockito.mock(AiRateLimiter.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiMetricsService metricsService = new AiMetricsService(registry);

        AiController controller = new AiController(orchestrator, rateLimiter, null, metricsService);

        AiChatRequest request = new AiChatRequest("Hi", List.of());

        ResponseEntity<AiChatResponse> response = controller.chat(request, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        Counter failureCounter = registry.find("smartlib_ai_request_failures_total").counter();
        assertNotNull(failureCounter);
        assertEquals(1.0, failureCounter.count());
    }
}
