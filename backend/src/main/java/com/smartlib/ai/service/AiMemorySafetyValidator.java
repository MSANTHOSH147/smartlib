package com.smartlib.ai.service;

import com.smartlib.ai.config.AiMemoryProperties;
import com.smartlib.ai.dto.UserMemoryDto;
import com.smartlib.enums.MemorySource;
import com.smartlib.enums.MemoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiMemorySafetyValidator {

    private final AiMemoryProperties properties;

    public static final int MAX_KEY_LENGTH = 100;
    public static final int MAX_VALUE_LENGTH = 500;

    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "password", "passwd", "pwd", "secret", "api_key", "apikey",
            "token", "jwt", "bearer", "credential", "credentials", "private_key",
            "auth_token", "credit_card", "cvv", "ssn", "social_security",
            "bank_account", "pin_number"
    );

    private static final Pattern JWT_PATTERN = Pattern.compile(
            "\\beyJ[a-zA-Z0-9_-]{10,}\\.[a-zA-Z0-9_-]{10,}\\.[a-zA-Z0-9_-]{10,}\\b"
    );

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "\\b(AIza[0-9A-Za-z-_]{30,}|gsk_[0-9A-Za-z]{15,}|sk-[0-9A-Za-z]{15,})\\b"
    );

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b"
    );

    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b\\d{3}-\\d{2}-\\d{4}\\b"
    );

    private static final Pattern DB_URI_PATTERN = Pattern.compile(
            "(?i)\\b(jdbc:[a-z:]+|mysql://|postgres://|mongodb://)\\S+"
    );

    public void validate(UserMemoryDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Memory DTO cannot be null.");
        }
        validate(
                dto.getMemoryType(),
                dto.getKey(),
                dto.getValue(),
                dto.getConfidence(),
                dto.getSource() != null ? dto.getSource() : MemorySource.CONVERSATION
        );
    }

    /**
     * Validates whether memory attributes meet safety, length, confidence, and non-sensitive requirements.
     * Throws IllegalArgumentException if validation fails.
     */
    public void validate(MemoryType type, String key, String value, Double confidence, MemorySource source) {
        if (type == null) {
            throw new IllegalArgumentException("Memory type cannot be null.");
        }

        if (key == null || key.trim().isBlank()) {
            throw new IllegalArgumentException("Memory key cannot be empty.");
        }

        String trimmedKey = key.trim();
        if (trimmedKey.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Memory key exceeds maximum allowed length of " + MAX_KEY_LENGTH + " characters.");
        }

        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("Memory value cannot be empty.");
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("Memory value exceeds maximum allowed length of " + MAX_VALUE_LENGTH + " characters.");
        }

        if (confidence == null) {
            throw new IllegalArgumentException("Memory confidence cannot be null.");
        }

        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Memory confidence must be between 0.0 and 1.0.");
        }

        double minConf = properties != null ? properties.getMinConfidence() : 0.75;
        if (confidence < minConf) {
            throw new IllegalArgumentException("Memory confidence " + confidence + " is below minimum allowed threshold of " + minConf + ".");
        }

        if (source == null) {
            throw new IllegalArgumentException("Memory source cannot be null.");
        }

        // Prohibit sensitive or credential information
        if (containsSensitiveData(trimmedKey) || containsSensitiveData(trimmedValue)) {
            log.warn("Blocked attempt to persist sensitive data in memory: key='{}'", trimmedKey);
            throw new IllegalArgumentException("Memory content contains prohibited sensitive or credential information.");
        }
    }

    /**
     * Checks if text contains passwords, JWTs, API keys, credentials, credit card numbers, or SSNs.
     */
    public boolean containsSensitiveData(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String lower = text.toLowerCase(Locale.ROOT);

        for (String keyword : SENSITIVE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }

        if (JWT_PATTERN.matcher(text).find()) {
            return true;
        }

        if (API_KEY_PATTERN.matcher(text).find()) {
            return true;
        }

        if (CREDIT_CARD_PATTERN.matcher(text).find()) {
            return true;
        }

        if (SSN_PATTERN.matcher(text).find()) {
            return true;
        }

        if (DB_URI_PATTERN.matcher(text).find()) {
            return true;
        }

        return false;
    }
}
