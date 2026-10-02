package com.smartlib.ai.security;

import com.smartlib.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for SmartLib AI chat requests.
 *
 * NOTE: This is an application-level in-memory limiter. In a distributed multi-instance
 * production deployment, this can be replaced with a distributed Redis or API Gateway rate limiter.
 */
@Component
@Slf4j
public class AiRateLimiter {

    public static final int MAX_REQUESTS_PER_MINUTE = 10;
    public static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_TRACKED_USERS = 10_000;

    private final int maxRequestsPerMinute;
    private final Map<String, Deque<Long>> userRequestTimestamps = new ConcurrentHashMap<>();

    public AiRateLimiter() {
        this(MAX_REQUESTS_PER_MINUTE);
    }

    public AiRateLimiter(@org.springframework.beans.factory.annotation.Value("${ai.rate-limit.per-minute:10}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute > 0 ? maxRequestsPerMinute : MAX_REQUESTS_PER_MINUTE;
    }

    /**
     * Checks if the request for the given user key is allowed under the rate limit.
     * Throws RateLimitExceededException if limit is exceeded.
     *
     * @param userKey unique identifier of the authenticated user
     */
    public void checkRateLimit(String userKey) {
        if (userKey == null || userKey.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MILLIS;

        Deque<Long> timestamps = userRequestTimestamps.compute(userKey, (k, existing) -> {
            Deque<Long> deque = (existing != null) ? existing : new ArrayDeque<>();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst() < windowStart) {
                    deque.pollFirst();
                }
                return deque;
            }
        });

        synchronized (timestamps) {
            if (timestamps.size() >= maxRequestsPerMinute) {
                log.warn("Rate limit exceeded for user key [{}]: {} requests in the last minute.",
                        maskUserKey(userKey), timestamps.size());
                throw new RateLimitExceededException(
                        "Rate limit exceeded: Maximum " + maxRequestsPerMinute + " AI chat requests per minute. Please try again later."
                );
            }
            timestamps.addLast(now);
        }

        if (userRequestTimestamps.size() > MAX_TRACKED_USERS) {
            cleanupExpiredEntries();
        }
    }

    /**
     * Periodic cleanup job every 60 seconds to purge idle user queues.
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredEntries() {
        long windowStart = System.currentTimeMillis() - WINDOW_MILLIS;
        userRequestTimestamps.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst() < windowStart) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
    }

    public void reset() {
        userRequestTimestamps.clear();
    }

    private String maskUserKey(String key) {
        if (key.length() <= 3) return "***";
        return key.substring(0, 2) + "***" + key.substring(key.length() - 1);
    }
}
