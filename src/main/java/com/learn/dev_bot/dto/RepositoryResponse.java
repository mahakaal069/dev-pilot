package com.learn.dev_bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learn.dev_bot.enumeration.IndexStatus;

import java.time.Instant;
import java.util.UUID;

public record RepositoryResponse(
        UUID id,
        Long githubRepoId,
        String owner,
        String name,
        String fullName,
        @JsonProperty("isPrivate") boolean isPrivate,
        String defaultBranch,
        String language,
        String htmlUrl,
        String description,
        IndexStatus indexStatus,
        Instant indexAt,
        Integer chunkCount,
        Integer filesTotal,
        Integer filesProcessed,
        String errorMessage
) {}

