package com.smartlib.ai.router;

import com.smartlib.ai.config.AiRoutingProperties;
import com.smartlib.ai.model.AiModelRequest;
import com.smartlib.ai.model.AiModelResponse;
import com.smartlib.ai.provider.AiModelProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class AiModelRouter {

    private final Map<String, AiModelProvider> providers = new ConcurrentHashMap<>();
    private final AiRoutingProperties routingProperties;
    private final Map<String, ProviderHealth> healthTrackers = new ConcurrentHashMap<>();

    public AiModelRouter(List<AiModelProvider> providerList, AiRoutingProperties routingProperties) {
        this.routingProperties = routingProperties;
        if (providerList != null) {
            for (AiModelProvider provider : providerList) {
                this.providers.put(provider.getProviderName().toLowerCase(Locale.ROOT), provider);
            }
        }
    }

    public AiModelResponse execute(AiModelRequest request) {
        AiModelProvider selectedProvider = selectProvider(request);
        boolean requestedWebGrounding = request != null && request.isUseWebGrounding();

        AiModelRequest actualRequest = request;
        boolean selectedSupportsGrounding = selectedProvider.getCapabilities() != null && selectedProvider.getCapabilities().isSupportsWebGrounding();
        if (requestedWebGrounding && !selectedSupportsGrounding) {
            log.warn("Web grounding was requested, but provider [{}] does not support it. Executing ungrounded.",
                    selectedProvider.getProviderName());
            actualRequest = request.toBuilder().useWebGrounding(false).build();
        }

        try {
            AiModelResponse response = selectedProvider.generateChat(actualRequest);
            recordSuccess(selectedProvider.getProviderName());
            return response;

        } catch (Exception ex) {
            if (!isProviderLevelFailure(ex)) {
                // Application-level error (e.g. invalid arguments, business logic) -> DO NOT fallback
                log.debug("Application-level error encountered on [{}], suppressing fallback: {}",
                        selectedProvider.getProviderName(), ex.getMessage());
                throw ex;
            }

            recordFailure(selectedProvider.getProviderName());
            log.warn("Provider [{}] encountered provider-level failure: {}",
                    selectedProvider.getProviderName(), ex.getMessage());

            // Step 10: Do NOT fallback mid-tool workflow if tool results have already been executed
            if (request != null && request.hasToolResults()) {
                log.error("Provider [{}] failed after tool execution started. Suppressing fallback to avoid duplicated side effects.",
                        selectedProvider.getProviderName());
                throw new RuntimeException("AI provider failed mid-tool workflow: " + ex.getMessage(), ex);
            }

            // Attempt fallback to secondary provider
            String fallbackName = routingProperties.getFallbackProvider();
            if (fallbackName != null && !fallbackName.equalsIgnoreCase(selectedProvider.getProviderName())) {
                AiModelProvider fallback = getProvider(fallbackName);
                if (fallback != null && fallback.isAvailable() && !isCoolingDown(fallbackName)) {
                    log.info("Executing fallback from [{}] to provider [{}]",
                            selectedProvider.getProviderName(), fallbackName);
                    try {
                        AiModelRequest fallbackRequest = request;
                        boolean fallbackSupportsGrounding = fallback.getCapabilities() != null && fallback.getCapabilities().isSupportsWebGrounding();
                        if (requestedWebGrounding && !fallbackSupportsGrounding) {
                            fallbackRequest = request.toBuilder().useWebGrounding(false).build();
                        }

                        AiModelResponse fallbackResponse = fallback.generateChat(fallbackRequest);
                        recordSuccess(fallbackName);

                        if (requestedWebGrounding && !fallbackSupportsGrounding) {
                            // Option A: Ensure fallback never claims fake web sources, and append note
                            List<com.smartlib.ai.model.AiSource> safeSources = fallbackResponse.getSources() != null
                                    ? fallbackResponse.getSources().stream()
                                            .filter(s -> !"WEB".equalsIgnoreCase(s.getType()))
                                            .toList()
                                    : Collections.emptyList();
                            String text = fallbackResponse.getText();
                            if (text != null && !text.isBlank()) {
                                text = text + "\n\n*(Note: Real-time web verification was unavailable for this query.)*";
                            }
                            fallbackResponse = fallbackResponse.toBuilder()
                                    .text(text)
                                    .sources(safeSources)
                                    .build();
                        }

                        return fallbackResponse;
                    } catch (Exception fallbackEx) {
                        recordFailure(fallbackName);
                        log.error("Fallback provider [{}] also failed: {}", fallbackName, fallbackEx.getMessage());
                        throw new RuntimeException("Both primary and fallback AI providers failed.", fallbackEx);
                    }
                } else {
                    log.warn("Fallback provider [{}] is either not found, not available, or cooling down.", fallbackName);
                }
            }

            throw ex;
        }
    }

    public AiModelProvider selectProvider(AiModelRequest request) {
        // If web grounding is requested, prefer a provider that explicitly supports it
        if (request != null && request.isUseWebGrounding()) {
            String primaryName = routingProperties.getPrimaryProvider();
            AiModelProvider primary = getProvider(primaryName);
            if (primary != null && primary.isAvailable() && !isCoolingDown(primaryName)
                    && primary.getCapabilities() != null && primary.getCapabilities().isSupportsWebGrounding()) {
                return primary;
            }

            // Look for any available provider that supports web grounding
            for (AiModelProvider provider : providers.values()) {
                if (provider.isAvailable() && !isCoolingDown(provider.getProviderName())
                        && provider.getCapabilities() != null && provider.getCapabilities().isSupportsWebGrounding()) {
                    log.info("Selected provider [{}] supporting web grounding for grounded request.",
                            provider.getProviderName());
                    return provider;
                }
            }
        }

        String primaryName = routingProperties.getPrimaryProvider();
        AiModelProvider primary = getProvider(primaryName);

        if (primary != null && primary.isAvailable() && !isCoolingDown(primaryName)) {
            return primary;
        }

        String fallbackName = routingProperties.getFallbackProvider();
        if (fallbackName != null && !fallbackName.equalsIgnoreCase(primaryName)) {
            AiModelProvider fallback = getProvider(fallbackName);
            if (fallback != null && fallback.isAvailable() && !isCoolingDown(fallbackName)) {
                log.info("Primary provider [{}] unavailable/cooling down. Selected fallback provider [{}]",
                        primaryName, fallbackName);
                return fallback;
            }
        }

        if (primary != null && primary.isAvailable()) {
            return primary;
        }

        throw new IllegalStateException("No AI model provider is currently available.");
    }

    public AiModelProvider getProvider(String providerName) {
        if (providerName == null) return null;
        return providers.get(providerName.toLowerCase(Locale.ROOT));
    }

    public boolean isProviderLevelFailure(Throwable ex) {
        if (ex == null) return false;
        Throwable current = ex;
        while (current != null) {
            if (current instanceof RestClientResponseException rcre) {
                int status = rcre.getStatusCode().value();
                if (status == 429 || (status >= 500 && status < 600)) {
                    return true;
                }
            }
            if (current instanceof ResourceAccessException
                    || current instanceof TimeoutException
                    || current instanceof SocketTimeoutException
                    || current instanceof ConnectException
                    || current instanceof UnknownHostException) {
                return true;
            }
            String msg = current.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase(Locale.ROOT);
                if (lower.contains("status: 429") || lower.contains("429 too many requests")
                        || lower.contains("status: 5") || lower.contains("500") || lower.contains("502")
                        || lower.contains("503") || lower.contains("504")
                        || lower.contains("timeout") || lower.contains("timed out")
                        || lower.contains("connection refused") || lower.contains("failed to communicate")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public ProviderHealth getProviderHealth(String providerName) {
        if (providerName == null) return null;
        return healthTrackers.computeIfAbsent(providerName.toLowerCase(Locale.ROOT), ProviderHealth::new);
    }

    public void recordSuccess(String providerName) {
        getProviderHealth(providerName).recordSuccess();
    }

    public void recordFailure(String providerName) {
        int cooldownSeconds = routingProperties.getCooldownSeconds() > 0 ? routingProperties.getCooldownSeconds() : 60;
        getProviderHealth(providerName).recordFailure(Duration.ofSeconds(cooldownSeconds));
    }

    public boolean isCoolingDown(String providerName) {
        return getProviderHealth(providerName).isCoolingDown();
    }

    public void resetHealth(String providerName) {
        getProviderHealth(providerName).reset();
    }

    @Getter
    public static class ProviderHealth {
        private final String providerName;
        private volatile int consecutiveFailures = 0;
        private volatile Instant lastFailureTime;
        private volatile Instant cooldownUntil;

        public ProviderHealth(String providerName) {
            this.providerName = providerName;
        }

        public synchronized void recordSuccess() {
            this.consecutiveFailures = 0;
            this.cooldownUntil = null;
        }

        public synchronized void recordFailure(Duration cooldownDuration) {
            this.consecutiveFailures++;
            this.lastFailureTime = Instant.now();
            if (cooldownDuration != null) {
                this.cooldownUntil = Instant.now().plus(cooldownDuration);
            }
        }

        public boolean isCoolingDown() {
            Instant until = this.cooldownUntil;
            return until != null && Instant.now().isBefore(until);
        }

        public synchronized void reset() {
            this.consecutiveFailures = 0;
            this.lastFailureTime = null;
            this.cooldownUntil = null;
        }
    }
}
