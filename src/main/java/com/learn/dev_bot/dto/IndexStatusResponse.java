package com.learn.dev_bot.dto;

import com.learn.dev_bot.enumeration.IndexStatus;

import java.time.Instant;
import java.util.UUID;

public record IndexStatusResponse(
        UUID repoId,
        IndexStatus indexStatus,
        Integer filesTotal,
        Integer filesProcessed,
        Integer chunkCount,
        Instant indexedAt,
        String errorMessage
) {}
