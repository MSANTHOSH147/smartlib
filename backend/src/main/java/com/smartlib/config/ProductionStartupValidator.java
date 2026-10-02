package com.smartlib.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates critical environment configuration at startup to prevent running
 * production environments with development or weak defaults.
 */
@Component
@Slf4j
public class ProductionStartupValidator {

    public static final String DEV_JWT_PLACEHOLDER = "smartlib-development-jwt-secret-key-min-256-bits-for-local-testing-only-replace-in-prod";

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @PostConstruct
    public void validateConfiguration() {
        boolean isProduction = isProductionEnvironment();

        if (isProduction) {
            log.info("Detected PRODUCTION environment profile. Enforcing strict security startup validation...");

            if (jwtSecret == null || jwtSecret.isBlank() || DEV_JWT_PLACEHOLDER.equals(jwtSecret)) {
                throw new IllegalStateException(
                        "CRITICAL PRODUCTION SECURITY ERROR: JWT_SECRET environment variable is missing or using default development secret. " +
                        "A secure random secret (min 256 bits / 32 characters) must be supplied via the JWT_SECRET environment variable in production."
                );
            }

            if (jwtSecret.length() < 32) {
                throw new IllegalStateException(
                        "CRITICAL PRODUCTION SECURITY ERROR: JWT_SECRET is too short. " +
                        "HMAC-SHA256 requires at least 32 characters (256 bits)."
                );
            }

            log.info("Production security startup validation passed successfully.");
        } else {
            if (DEV_JWT_PLACEHOLDER.equals(jwtSecret) || jwtSecret == null || jwtSecret.isBlank()) {
                log.warn("DEVELOPMENT NOTICE: Running with local development JWT secret. Set JWT_SECRET environment variable for production.");
            }
        }
    }

    private boolean isProductionEnvironment() {
        String env = System.getenv("ENVIRONMENT");
        String render = System.getenv("RENDER");
        String profiles = activeProfile != null ? activeProfile.toLowerCase() : "";

        return "production".equalsIgnoreCase(env)
                || "prod".equalsIgnoreCase(env)
                || "true".equalsIgnoreCase(render)
                || profiles.contains("prod")
                || profiles.contains("production");
    }
}
