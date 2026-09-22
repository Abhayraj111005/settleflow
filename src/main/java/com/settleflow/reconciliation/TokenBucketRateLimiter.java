package com.settleflow.reconciliation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBucketRateLimiter {

    private final int capacity;
    private final double refillRatePerSecond;

    private final Map<String, Bucket> buckets =
            new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(
            @Value("${settleflow.rate-limit.capacity:5}")
            int capacity,
            @Value("${settleflow.rate-limit.refill-rate:1}")
            double refillRatePerSecond) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Rate limit capacity must be greater than zero"
            );
        }

        if (refillRatePerSecond <= 0) {
            throw new IllegalArgumentException(
                    "Rate limit refill rate must be greater than zero"
            );
        }

        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    public boolean isAllowed(String clientId) {

        Bucket bucket = buckets.computeIfAbsent(
                clientId,
                ignored -> new Bucket(
                        capacity,
                        System.nanoTime()
                )
        );

        return bucket.tryConsume();
    }

    private class Bucket {

        private double tokens;
        private long lastRefillTime;

        private Bucket(
                int initialTokens,
                long lastRefillTime) {

            this.tokens = initialTokens;
            this.lastRefillTime = lastRefillTime;
        }

        private synchronized boolean tryConsume() {

            refill();

            if (tokens < 1) {
                return false;
            }

            tokens--;

            return true;
        }

        private void refill() {

            long now = System.nanoTime();

            long elapsedNanos =
                    now - lastRefillTime;

            double elapsedSeconds =
                    elapsedNanos / 1_000_000_000.0;

            double tokensToAdd =
                    elapsedSeconds * refillRatePerSecond;

            if (tokensToAdd > 0) {

                tokens = Math.min(
                        capacity,
                        tokens + tokensToAdd
                );

                lastRefillTime = now;
            }
        }
    }
}