package com.smartlib.ai;

import com.smartlib.ai.security.AiRateLimiter;
import com.smartlib.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRateLimiterTest {

    private AiRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new AiRateLimiter();
    }

    @Test
    @DisplayName("J. Rate limit allows normal traffic within limit (10 req/min)")
    void testRateLimitAllowsNormalTraffic() {
        String userKey = "user:101";

        // First 10 requests should succeed without exception
        for (int i = 0; i < AiRateLimiter.MAX_REQUESTS_PER_MINUTE; i++) {
            rateLimiter.checkRateLimit(userKey);
        }
    }

    @Test
    @DisplayName("K. Rate limit rejects excessive requests with RateLimitExceededException")
    void testRateLimitRejectsExcessiveRequests() {
        String userKey = "user:102";

        for (int i = 0; i < AiRateLimiter.MAX_REQUESTS_PER_MINUTE; i++) {
            rateLimiter.checkRateLimit(userKey);
        }

        // 11th request in the same window must be rejected
        assertThatThrownBy(() -> rateLimiter.checkRateLimit(userKey))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Maximum 10 AI chat requests per minute");
    }

    @Test
    @DisplayName("L. Rate limiting is isolated per authenticated user")
    void testRateLimitIsIsolatedPerUser() {
        String user1 = "user:201";
        String user2 = "user:202";

        // User 1 exhausts all 10 requests
        for (int i = 0; i < AiRateLimiter.MAX_REQUESTS_PER_MINUTE; i++) {
            rateLimiter.checkRateLimit(user1);
        }

        // User 1 is blocked
        assertThatThrownBy(() -> rateLimiter.checkRateLimit(user1))
                .isInstanceOf(RateLimitExceededException.class);

        // User 2 should NOT be blocked and can make requests normally
        for (int i = 0; i < AiRateLimiter.MAX_REQUESTS_PER_MINUTE; i++) {
            rateLimiter.checkRateLimit(user2);
        }

        // Only when User 2 exceeds their own limit are they blocked
        assertThatThrownBy(() -> rateLimiter.checkRateLimit(user2))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("Rate limiter cleanup purges expired entries without error")
    void testCleanupExpiredEntries() {
        rateLimiter.checkRateLimit("user:301");
        rateLimiter.cleanupExpiredEntries();
        rateLimiter.reset();
    }
}
