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

    @Autowired
    public AiController(SmartLibAiOrchestrator aiOrchestrator,
                        AiRateLimiter rateLimiter,
                        @Autowired(required = false) UserMemoryService userMemoryService,
                        @Autowired(required = false) com.smartlib.ai.observability.AiMetricsService metricsService) {
        this.aiOrchestrator = aiOrchestrator;
        this.rateLimiter = rateLimiter;
        this.userMemoryService = userMemoryService;
        this.metricsService = metricsService != null ? metricsService : new com.smartlib.ai.observability.AiMetricsService(null);
    }

    public AiController(SmartLibAiOrchestrator aiOrchestrator,
                        AiRateLimiter rateLimiter,
                        UserMemoryService userMemoryService) {
        this(aiOrchestrator, rateLimiter, userMemoryService, null);
    }

    public AiController(SmartLibAiOrchestrator aiOrchestrator, AiRateLimiter rateLimiter) {
        this(aiOrchestrator, rateLimiter, null, null);
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            Authentication authentication) {

        String requestId = java.util.UUID.randomUUID().toString();
        long startTime = System.nanoTime();

        com.smartlib.ai.observability.AiRequestTrace trace = com.smartlib.ai.observability.AiRequestTrace.builder()
                .requestId(requestId)
                .provider("gemini")
                .model("gemini-2.5-flash")
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

            trace.setTotalLatencyMs(totalLatencyMs);
            trace.setSuccess(result.isSuccess());
            trace.setToolCount(result.getToolCallsExecuted() != null ? result.getToolCallsExecuted().size() : 0);
            trace.setToolNames(result.getToolCallsExecuted() != null ? result.getToolCallsExecuted() : Collections.emptyList());
            trace.setWebGroundingUsed(result.getSources() != null && result.getSources().stream().anyMatch(s -> "WEB".equalsIgnoreCase(s.getType())));
            trace.setMemoryUsed(request.getMessage() != null && (request.getMessage().toLowerCase().contains("recommend") || request.getMessage().toLowerCase().contains("favorite")));
            trace.setErrorCategory(result.isSuccess() ? com.smartlib.ai.observability.AiErrorCategory.NONE : com.smartlib.ai.observability.AiErrorCategory.INTERNAL);

            log.info("{}", trace.toStructuredLog());
            metricsService.recordRequest(trace);

            AiChatResponse response = AiChatResponse.of(
                    result.getReply(),
                    result.isSuccess(),
                    result.getToolCallsExecuted(),
                    result.getSources()
            );

            if (!result.isSuccess()) {
                trace.setErrorCategory(com.smartlib.ai.observability.AiErrorCategory.PROVIDER_UNAVAILABLE);
                log.warn("AI chat orchestration unfulfilled: requestId={}", requestId);
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
            trace.setErrorCategory(com.smartlib.ai.observability.AiErrorCategory.INTERNAL);
            log.warn("{}", trace.toStructuredLog());
            metricsService.recordRequest(trace);
            throw ex;
        }
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
