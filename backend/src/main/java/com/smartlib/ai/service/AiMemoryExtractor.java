package com.smartlib.ai.service;

import com.smartlib.ai.dto.UserMemoryDto;
import com.smartlib.enums.MemorySource;
import com.smartlib.enums.MemoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class AiMemoryExtractor {

    private final AiMemorySafetyValidator safetyValidator;

    public AiMemoryExtractor(AiMemorySafetyValidator safetyValidator) {
        this.safetyValidator = safetyValidator;
    }

    public AiMemoryExtractor() {
        this(new AiMemorySafetyValidator(new com.smartlib.ai.config.AiMemoryProperties()));
    }

    public List<UserMemoryDto> extractMemories(String message) {
        return extractCandidateMemories(message);
    }

    // Pattern: "Remember that I (like|prefer|love|enjoy) ..."
    private static final Pattern EXPLICIT_REMEMBER_PATTERN = Pattern.compile(
            "(?i)\\bremember\\s+that\\s+I\\s+(?:mostly\\s+)?(?:like|prefer|love|enjoy)\\s+([^.!?]+)"
    );

    // Pattern: "My favorite author is ..." or "My favorite writer is ..."
    private static final Pattern FAVORITE_AUTHOR_PATTERN = Pattern.compile(
            "(?i)\\bmy\\s+favorite\\s+(?:author|writer)\\s+is\\s+([A-Za-z\\s.'-]+)"
    );

    // Pattern: "I (mostly )?(like|prefer|love) books by ..."
    private static final Pattern AUTHOR_PREFERENCE_PATTERN = Pattern.compile(
            "(?i)\\bI\\s+(?:mostly\\s+)?(?:like|prefer|love)\\s+books\\s+by\\s+([A-Za-z\\s.'-]+)"
    );

    // Pattern: "I (mostly )?(like|prefer|love) ... books"
    private static final Pattern TOPIC_PREFERENCE_PATTERN = Pattern.compile(
            "(?i)\\bI\\s+(?:mostly\\s+)?(?:like|prefer|love)\\s+([A-Za-z0-9\\s#-]+)\\s+books?\\b"
    );

    // Pattern: "I prefer (short )?books with ..."
    private static final Pattern STYLE_PREFERENCE_PATTERN = Pattern.compile(
            "(?i)\\bI\\s+prefer\\s+(?:(?:short|long)\\s+)?books\\s+with\\s+([^.!?]+)"
    );

    // Transient queries that must never trigger memory extraction
    private static final List<String> TRANSIENT_SIGNALS = List.of(
            "what books do you have", "what do you have", "is it available",
            "is clean code available", "how many copies", "do we have",
            "recommend something", "recommend me", "what are my",
            "i am currently reading", "i'm currently reading",
            "who wrote", "what is", "where is", "can i borrow",
            "forget", "don't remember", "do not remember", "what do you remember"
    );

    /**
     * Conservatively extracts candidate persistent preferences from user chat messages.
     * Returns an empty list if message does not contain persistent intent.
     */
    public List<UserMemoryDto> extractCandidateMemories(String message) {
        if (message == null || message.trim().isBlank()) {
            return Collections.emptyList();
        }

        String trimmed = message.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        // Check if message is a transient catalog or status query
        for (String signal : TRANSIENT_SIGNALS) {
            if (lower.startsWith(signal) || lower.contains(signal)) {
                return Collections.emptyList();
            }
        }

        List<UserMemoryDto> candidates = new ArrayList<>();

        // 1. "Remember that I prefer/like/love ..."
        Matcher remMatcher = EXPLICIT_REMEMBER_PATTERN.matcher(trimmed);
        if (remMatcher.find()) {
            String val = cleanExtractedValue(remMatcher.group(1));
            if (isValidCandidateValue(val)) {
                candidates.add(UserMemoryDto.builder()
                        .memoryType(MemoryType.PREFERENCE)
                        .key(inferKeyFromValue(val, "preferred_preference"))
                        .value(val)
                        .confidence(0.95)
                        .source(MemorySource.EXPLICIT_USER)
                        .build());
            }
        }

        // 2. "My favorite author is [Name]"
        Matcher favAuthMatcher = FAVORITE_AUTHOR_PATTERN.matcher(trimmed);
        if (favAuthMatcher.find()) {
            String author = cleanExtractedValue(favAuthMatcher.group(1));
            if (isValidCandidateValue(author)) {
                candidates.add(UserMemoryDto.builder()
                        .memoryType(MemoryType.AUTHOR)
                        .key("favorite_author")
                        .value(author)
                        .confidence(0.90)
                        .source(MemorySource.CONVERSATION)
                        .build());
            }
        }

        // 3. "I love books by [Name]"
        Matcher authByMatcher = AUTHOR_PREFERENCE_PATTERN.matcher(trimmed);
        if (authByMatcher.find()) {
            String author = cleanExtractedValue(authByMatcher.group(1));
            if (isValidCandidateValue(author)) {
                candidates.add(UserMemoryDto.builder()
                        .memoryType(MemoryType.AUTHOR)
                        .key("preferred_author")
                        .value(author)
                        .confidence(0.85)
                        .source(MemorySource.CONVERSATION)
                        .build());
            }
        }

        // 4. "I prefer books with [practical examples / diagrams]"
        Matcher styleMatcher = STYLE_PREFERENCE_PATTERN.matcher(trimmed);
        if (styleMatcher.find()) {
            String style = cleanExtractedValue(styleMatcher.group(1));
            if (isValidCandidateValue(style)) {
                candidates.add(UserMemoryDto.builder()
                        .memoryType(MemoryType.STYLE)
                        .key("reading_style")
                        .value(style)
                        .confidence(0.85)
                        .source(MemorySource.CONVERSATION)
                        .build());
            }
        }

        // 5. "I mostly like Java and system design books" or "I like Java books"
        Matcher topicMatcher = TOPIC_PREFERENCE_PATTERN.matcher(trimmed);
        if (topicMatcher.find() && candidates.isEmpty()) {
            String topicRaw = cleanExtractedValue(topicMatcher.group(1));
            if (isValidCandidateValue(topicRaw)) {
                if (topicRaw.contains(" and ")) {
                    String[] parts = topicRaw.split(" and ");
                    for (String part : parts) {
                        String cleanPart = cleanExtractedValue(part);
                        if (isValidCandidateValue(cleanPart)) {
                            candidates.add(UserMemoryDto.builder()
                                    .memoryType(isCommonCategory(cleanPart) ? MemoryType.CATEGORY : MemoryType.INTEREST)
                                    .key(isCommonCategory(cleanPart) ? "preferred_category" : "preferred_topic")
                                    .value(cleanPart)
                                    .confidence(0.85)
                                    .source(MemorySource.CONVERSATION)
                                    .build());
                        }
                    }
                } else {
                    candidates.add(UserMemoryDto.builder()
                            .memoryType(isCommonCategory(topicRaw) ? MemoryType.CATEGORY : MemoryType.INTEREST)
                            .key(isCommonCategory(topicRaw) ? "preferred_category" : "preferred_topic")
                            .value(topicRaw)
                            .confidence(0.85)
                            .source(MemorySource.CONVERSATION)
                            .build());
                }
            }
        }

        // Filter out any candidates that fail safety inspection
        return candidates.stream()
                .filter(dto -> !safetyValidator.containsSensitiveData(dto.getKey())
                        && !safetyValidator.containsSensitiveData(dto.getValue()))
                .toList();
    }

    private String cleanExtractedValue(String val) {
        if (val == null) return "";
        return val.replaceAll("[,.!?]+$", "").trim();
    }

    private boolean isValidCandidateValue(String val) {
        return val != null && !val.isBlank() && val.length() >= 2 && val.length() <= 100;
    }

    private boolean isCommonCategory(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("java") || lower.contains("python") || lower.contains("javascript")
                || lower.contains("fiction") || lower.contains("science") || lower.contains("history")
                || lower.contains("engineering") || lower.contains("design") || lower.contains("architecture");
    }

    private String inferKeyFromValue(String val, String defaultKey) {
        String lower = val.toLowerCase(Locale.ROOT);
        if (lower.contains("practical") || lower.contains("example") || lower.contains("short") || lower.contains("concise")) {
            return "reading_style";
        }
        if (isCommonCategory(lower)) {
            return "preferred_category";
        }
        return defaultKey;
    }
}
