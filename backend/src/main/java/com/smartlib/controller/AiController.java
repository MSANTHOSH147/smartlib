package com.smartlib.controller;

import com.smartlib.ai.dto.AiChatRequest;
import com.smartlib.ai.dto.AiChatResponse;
import com.smartlib.ai.dto.AiOrchestrationResult;
import com.smartlib.ai.dto.gemini.Content;
import com.smartlib.ai.security.AiRateLimiter;
import com.smartlib.ai.service.SmartLibAiOrchestrator;
import com.smartlib.entity.User;
import com.smartlib.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.smartlib.ai.dto.UserMemoryDto;
import com.smartlib.ai.service.UserMemoryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the secure SmartLib AI assistant chat endpoint and memory management.
 *
 * Endpoints:
 * - POST /api/ai/chat
 * - GET /api/ai/memory
 * - DELETE /api/ai/memory/{id}
 * Requires an authenticated member or admin user (JWT bearer token).
 */
@RestController
@RequestMapping("/api/ai")
@Slf4j
public class AiController {

    private final SmartLibAiOrchestrator aiOrchestrator;
    private final AiRateLimiter rateLimiter;
    private final UserMemoryService userMemoryService;
    private final com.smartlib.ai.observability.AiMetricsService metricsService;
    private final com.smartlib.ai.config.GeminiAiProperties geminiProperties;

    @Autowired
    public AiController(SmartLibAiOrchestrator aiOrchestrator,
                        AiRateLimiter rateLimiter,
                        @Autowired(required = false) UserMemoryService userMemoryService,
                        @Autowired(required = false) com.smartlib.ai.observability.AiMetricsService metricsService,
                        @Autowired(required = false) com.smartlib.ai.config.GeminiAiProperties geminiProperties) {
        this.aiOrchestrator = aiOrchestrator;
        this.rateLimiter = rateLimiter;
        this.userMemoryService = userMemoryService;
        this.metricsService = metricsService != null ? metricsService : new com.smartlib.ai.observability.AiMetricsService(null);
        this.geminiProperties = geminiProperties;
    }

    public AiController(SmartLibAiOrchestrator aiOrchestrator,
                        AiRateLimiter rateLimiter,
                        UserMemoryService userMemoryService,
                        com.smartlib.ai.observability.AiMetricsService metricsService) {
        this(aiOrchestrator, rateLimiter, userMemoryService, metricsService, null);
    }

    public AiController(SmartLibAiOrchestrator aiOrchestrator,
                        AiRateLimiter rateLimiter,
                        UserMemoryService userMemoryService) {
        this(aiOrchestrator, rateLimiter, userMemoryService, null, null);
    }

    public AiController(SmartLibAiOrchestrator aiOrchestrator, AiRateLimiter rateLimiter) {
        this(aiOrchestrator, rateLimiter, null, null, null);
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            Authentication authentication) {

        String requestId = java.util.UUID.randomUUID().toString();
        long startTime = System.nanoTime();

        String configuredModel = geminiProperties != null && geminiProperties.getModel() != null
                ? geminiProperties.getModel()
                : "gemini-3.8-flash";

        com.smartlib.ai.observability.AiRequestTrace trace = com.smartlib.ai.observability.AiRequestTrace.builder()
                .requestId(requestId)
                .provider("gemini")
                .model(configuredModel)
                .build();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthenticated AI chat attempt rejected.");
            trace.setSuccess(false);
            trace.setErrorCategory(com.smartlib.ai.observability.AiErrorCategory.AUTHENTICATION);
            trace.setTotalLatencyMs((System.nanoTime() - startTime) / 1_000_000);
            metricsService.recordRequest(trace);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userKey = resolveUserKey(authentication);
        if (userKey == null || userKey.isBlank()) {
            log.warn("Unable to resolve authenticated user identity for AI chat.");
            trace.setSuccess(false);
            trace.setErrorCategory(com.smartlib.ai.observability.AiErrorCategory.AUTHENTICATION);
            trace.setTotalLatencyMs((System.nanoTime() - startTime) / 1_000_000);
            metricsService.recordRequest(trace);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Enforce per-user sliding window rate limiting (10 req/min)
        try {
            rateLimiter.checkRateLimit(userKey);
        } catch (Exception ex) {
            trace.setSuccess(false);
            trace.setErrorCategory(com.smartlib.ai.observability.AiErrorCategory.RATE_LIMIT);
            trace.setTotalLatencyMs((System.nanoTime() - startTime) / 1_000_000);
            metricsService.recordRequest(trace);
            throw ex;
        }

        log.info("AI chat request accepted for user [{}], message length={}",
                maskUserKey(userKey), request.getMessage().length());

        // Validate and convert chat history structure
        List<Content> historyContents = request.toContentList();

        try {
            // Execute orchestration
            AiOrchestrationResult result = aiOrchestrator.chat(request.getMessage(), historyContents);
            long totalLatencyMs = (System.nanoTime() - startTime) / 1_000_000;

            if (result.getProvider() != null) {
                trace.setProvider(result.getProvider());
            }
            if (result.getModel() != null) {
                trace.setModel(result.getModel());
            }
            trace.setFallbackUsed(result.isFallbackUsed());

            trace.setTotalLatencyMs(totalLatencyMs);
            trace.setSuccess(result.isSuccess());
            trace.setToolCount(result.getToolCallsExecuted() != null ? result.getToolCallsExecuted().size() : 0);
            trace.setToolNames(result.getToolCallsExecuted() != null ? result.getToolCallsExecuted() : Collections.emptyList());
            trace.setWebGroundingUsed(result.getSources() != null && result.getSources().stream().anyMatch(s -> "WEB".equalsIgnoreCase(s.getType())));
            trace.setMemoryUsed(request.getMessage() != null && (request.getMessage().toLowerCase().contains("recommend") || request.getMessage().toLowerCase().contains("favorite")));
            trace.setErrorCategory(result.isSuccess() ? com.smartlib.ai.observability.AiErrorCategory.NONE : determineErrorCategory(result.getErrorMessage()));

            log.info("{}", trace.toStructuredLog());
            metricsService.recordRequest(trace);

            AiChatResponse response = AiChatResponse.of(
                    result.getReply(),
                    result.isSuccess(),
                    result.getToolCallsExecuted(),
                    result.getSources()
            );

            if (!result.isSuccess()) {
                log.warn("AI chat orchestration unfulfilled: requestId={}, errorCategory={}", requestId, trace.getErrorCategory());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .header("X-AI-Request-Id", requestId)
                        .body(response);
            }

            return ResponseEntity.ok()
                    .header("X-AI-Request-Id", requestId)
                    .body(response);
        } catch (Exception ex) {
            long totalLatencyMs = (System.nanoTime() - startTime) / 1_000_000;
            trace.setTotalLatencyMs(totalLatencyMs);
            trace.setSuccess(false);
            trace.setErrorCategory(determineErrorCategory(ex.getMessage()));
            log.warn("{}", trace.toStructuredLog());
            metricsService.recordRequest(trace);
            throw ex;
        }
    }

    private com.smartlib.ai.observability.AiErrorCategory determineErrorCategory(String errorMessage) {
        if (errorMessage == null) {
            return com.smartlib.ai.observability.AiErrorCategory.PROVIDER_UNAVAILABLE;
        }
        String lower = errorMessage.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("400") || lower.contains("bad request") || lower.contains("not supported")) {
            return com.smartlib.ai.observability.AiErrorCategory.PROVIDER_BAD_REQUEST;
        }
        if (lower.contains("401") || lower.contains("403") || lower.contains("unauthorized") || lower.contains("forbidden")) {
            return com.smartlib.ai.observability.AiErrorCategory.PROVIDER_AUTH;
        }
        if (lower.contains("404") || lower.contains("not found")) {
            return com.smartlib.ai.observability.AiErrorCategory.PROVIDER_NOT_FOUND;
        }
        if (lower.contains("429") || lower.contains("too many requests") || lower.contains("rate limit")) {
            return com.smartlib.ai.observability.AiErrorCategory.PROVIDER_RATE_LIMIT;
        }
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return com.smartlib.ai.observability.AiErrorCategory.PROVIDER_TIMEOUT;
        }
        if (lower.contains("500") || lower.contains("502") || lower.contains("503") || lower.contains("504")
                || lower.contains("unavailable") || lower.contains("offline")) {
            return com.smartlib.ai.observability.AiErrorCategory.PROVIDER_UNAVAILABLE;
        }
        return com.smartlib.ai.observability.AiErrorCategory.INTERNAL;
    }

    @GetMapping("/memory")
    public ResponseEntity<List<UserMemoryDto>> getMemories(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthenticated AI memory retrieval attempt rejected.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userKey = resolveUserKey(authentication);
        if (userKey == null || userKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        rateLimiter.checkRateLimit(userKey);

        if (userMemoryService == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<UserMemoryDto> dtos = userMemoryService.getActiveMemories();
        if (dtos == null) {
            dtos = Collections.emptyList();
        }

        log.info("Active memories retrieved for user [{}], count={}", maskUserKey(userKey), dtos.size());
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/memory/{id}")
    public ResponseEntity<Map<String, Object>> deleteMemory(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthenticated AI memory deletion attempt rejected.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userKey = resolveUserKey(authentication);
        if (userKey == null || userKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        rateLimiter.checkRateLimit(userKey);

        if (userMemoryService == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Memory service unavailable"));
        }

        boolean deactivated = userMemoryService.deactivateMemory(id);
        if (deactivated) {
            log.info("Memory ID [{}] deactivated for user [{}]", id, maskUserKey(userKey));
            return ResponseEntity.ok(Map.of("message", "Memory forgotten successfully", "id", id));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Memory not found or not owned by user"));
        }
    }

    private String resolveUserKey(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user && user.getId() != null) {
            return "user:" + user.getId();
        }
        String name = authentication.getName();
        if (name != null && !name.isBlank()) {
            return "user:" + name;
        }
        return null;
    }

    private String maskUserKey(String key) {
        if (key == null) return "unknown";
        if (key.length() <= 6) return "***";
        return key.substring(0, 5) + "***";
    }
}
