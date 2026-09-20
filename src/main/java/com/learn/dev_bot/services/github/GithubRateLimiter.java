package com.learn.dev_bot.services.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GithubRateLimiter {
    private final long delayMs;

    public GithubRateLimiter(@Value("${app.github.api-delay-ms:50}") long delayMs) {
        this.delayMs = delayMs;
    }

    public void pause() {
        if(delayMs <= 0)
            return;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while rate limiter");
        }
    }
}
