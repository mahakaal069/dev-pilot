package com.learn.dev_bot.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        long githubId,
        String githubUsername,
        String displayName,
        String avatarUrl
) {}
