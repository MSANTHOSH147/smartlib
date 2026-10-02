package com.smartlib.ai.service;

import com.smartlib.ai.config.AiWebGroundingProperties;
import com.smartlib.ai.dto.gemini.GeminiChatResponse;
import com.smartlib.ai.model.AiSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiWebGroundingService {

    private final AiWebGroundingProperties properties;

    private static final List<String> WEB_INTENT_SIGNALS = List.of(
            "latest", "current", "recent", "recently", "news", "today",
            "this year", "release", "new book", "newest", "upcoming",
            "recently published", "current author", "current edition",
            "current price", "current release", "current information",
            "official website", "recent announcement", "published in 202"
    );

    private static final List<String> SMARTLIB_INTERNAL_TERMS = List.of(
            "in smartlib", "at smartlib", "in our catalog", "in the library",
            "do we have", "is it available", "available copies", "shelf location",
            "my borrowings", "my fines", "my reservations", "my overdue books"
    );

    private static final Pattern SAFE_HTTP_SCHEME = Pattern.compile("^https?://.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_SCHEMES = Pattern.compile("^(javascript|data|vbscript|file):.*", Pattern.CASE_INSENSITIVE);

    public boolean isEnabled() {
        return properties != null && properties.isEnabled();
    }

    /**
     * Determines whether a user query should enable web grounding.
     * Rules:
     * 1. If web grounding is disabled globally, return false.
     * 2. If the query asks for current/latest/recent/news information, return true.
     * 3. If the query is purely SmartLib internal without current/external intent, return false.
     * 4. If the query is mixed (SmartLib + latest author book), return true.
     * 5. If the query is general static knowledge without current signals, return false.
     */
    public boolean shouldEnableWebGrounding(String message) {
        if (!isEnabled() || message == null || message.trim().isBlank()) {
            return false;
        }

        String lower = message.trim().toLowerCase(Locale.ROOT);

        boolean hasWebSignal = WEB_INTENT_SIGNALS.stream().anyMatch(lower::contains);
        boolean hasInternalSignal = SMARTLIB_INTERNAL_TERMS.stream().anyMatch(lower::contains);

        if (hasWebSignal) {
            // Either purely external current query or mixed query (SmartLib + latest)
            return true;
        }

        // Pure internal or general static knowledge without current signals
        return false;
    }

    /**
     * Sanitizes and transforms Gemini grounding metadata into safe, structured AiSource objects.
     */
    public List<AiSource> extractSources(GeminiChatResponse response) {
        if (response == null) {
            return Collections.emptyList();
        }

        GeminiChatResponse.GroundingMetadata metadata = response.extractGroundingMetadata();
        if (metadata == null || metadata.getGroundingChunks() == null || metadata.getGroundingChunks().isEmpty()) {
            return Collections.emptyList();
        }

        List<AiSource> sources = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        int maxResults = properties.getMaxResults() > 0 ? properties.getMaxResults() : 5;

        for (GeminiChatResponse.GroundingChunk chunk : metadata.getGroundingChunks()) {
            if (chunk == null || chunk.getWeb() == null) {
                continue;
            }

            String uri = chunk.getWeb().getUri();
            String title = chunk.getWeb().getTitle();

            if (!isValidWebUrl(uri)) {
                log.warn("Discarding invalid or unsafe grounding URL: {}", uri);
                continue;
            }

            String cleanUri = uri.trim();
            if (seenUrls.contains(cleanUri)) {
                continue;
            }
            seenUrls.add(cleanUri);

            String domain = extractDomain(cleanUri);
            String safeTitle = (title != null && !title.isBlank()) ? title.trim() : domain;

            sources.add(AiSource.web(safeTitle, cleanUri, domain));

            if (sources.size() >= maxResults) {
                break;
            }
        }

        return sources;
    }

    /**
     * Validates that a source URL uses a safe HTTP or HTTPS scheme and is structurally valid.
     */
    public static boolean isValidWebUrl(String url) {
        if (url == null || url.trim().isBlank()) {
            return false;
        }

        String trimmed = url.trim();

        if (UNSAFE_SCHEMES.matcher(trimmed).matches()) {
            return false;
        }

        if (!SAFE_HTTP_SCHEME.matcher(trimmed).matches()) {
            return false;
        }

        try {
            URI parsed = URI.create(trimmed);
            String scheme = parsed.getScheme();
            String host = parsed.getHost();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && host != null && !host.isBlank();
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Extracts domain host from a valid URL string.
     */
    public static String extractDomain(String url) {
        try {
            URI parsed = URI.create(url.trim());
            String host = parsed.getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {}
        return "web";
    }
}
