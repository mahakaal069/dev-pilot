package com.learn.dev_bot.services;

import com.learn.dev_bot.dto.IndexStatusResponse;
import com.learn.dev_bot.dto.RepositoryResponse;
import com.learn.dev_bot.entity.Repository;
import com.learn.dev_bot.entity.Users;
import com.learn.dev_bot.exceptions.NotFoundException;
import com.learn.dev_bot.repository.RepoRepository;
import com.learn.dev_bot.services.github.GithubApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepoService {

    private final RepoRepository repository;
    private final UsersService usersService;
    private final GithubApiClient githubApiClient;

    @Transactional
    public List<RepositoryResponse> syncAndListRepos(UUID userId) {

        Users user = usersService.requiredById(userId);
        String accessToken = usersService.decryptAccessToken(user);
        List<Map<String, Object>> remoteRepos = githubApiClient.listUserRepo(accessToken);

        List<Repository> saved = new ArrayList<>();

        for(Map<String, Object> remote : remoteRepos) {
            Long githubRepoId = toLong(remote.get("id"));
            Repository repo = repository.findByUserIdAndGithubRepoId(userId, githubRepoId)
                    .orElseGet(Repository::new);
            String fullName = String.valueOf(remote.get("full_name"));
            String[] parts = fullName.split("/", 2);

            repo.setUserId(userId);
            repo.setGithubRepoId(githubRepoId);
            repo.setOwner(parts.length > 0 ? parts[0] : String.valueOf(remote.get("owner")));
            repo.setName(parts.length > 1 ? parts[1] : String.valueOf(remote.get("name")));
            repo.setFullName(fullName);
            repo.setDefaultBranch(remote.get("default_branch") != null ? String.valueOf(remote.get("default_branch")) : "main");
            repo.setLanguage(remote.get("language") != null ? String.valueOf(remote.get("language")) : null);
            repo.setHtmlUrl(remote.get("html_url") != null ? String.valueOf(remote.get("html_url")) : null);
            repo.setDescription(remote.get("description") != null ? String.valueOf(remote.get("description")) : null);
            repo.setUpdatedAt(Instant.now());
            if(repo.getOwner() == null || repo.getOwner().isBlank()) {
                Object ownerObject = remote.get("owner");
                if(ownerObject instanceof Map<?, ?> ownerMap) {
                    if(ownerMap.get("login") != null)
                        repo.setOwner(String.valueOf(ownerMap.get("login")));
                }
            }

            saved.add(repository.save(repo));
        }

        return saved.stream()
                .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> listSorted(UUID userId) {
        return repository.findByUserIdOrderByFullNameAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Repository requiredOwned(UUID repoId, UUID userId) {
        return repository.findByIdAndUserId(repoId, userId).orElseThrow(
                () -> new NotFoundException("Repository not found."));
    }

    @Transactional(readOnly = true)
    public IndexStatusResponse status(UUID repoId, UUID userId) {
        Repository repo = requiredOwned(repoId, userId);

        return new IndexStatusResponse(
          repo.getId(),
          repo.getIndexStatus(),
          repo.getFilesTotal(),
          repo.getFilesProcessed(),
          repo.getChunkCount(),
          repo.getIndexedAt(),
          repo.getErrorMessages()
        );
    }

    public RepositoryResponse toResponse(Repository repo) {
        return new RepositoryResponse(
                repo.getId(),
                repo.getGithubRepoId(),
                repo.getOwner(),
                repo.getName(),
                repo.getFullName(),
                repo.getIsPrivate(),
                repo.getDefaultBranch(),
                repo.getLanguage(),
                repo.getHtmlUrl(),
                repo.getDescription(),
                repo.getIndexStatus(),
                repo.getCreatedAt(),
                repo.getChunkCount(),
                repo.getFilesTotal(),
                repo.getFilesProcessed(),
                repo.getErrorMessages()
        );
    }

    private static Long toLong(Object value) {
        if(value instanceof Number number)
            return number.longValue();

        return Long.parseLong(String.valueOf(value));
    }
}
