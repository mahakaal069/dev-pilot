package com.learn.dev_bot.services;

import com.learn.dev_bot.entity.Repository;
import com.learn.dev_bot.enumeration.IndexStatus;
import com.learn.dev_bot.exceptions.BadRequestException;
import com.learn.dev_bot.exceptions.NotFoundException;
import com.learn.dev_bot.repository.RepoRepository;
import com.learn.dev_bot.services.github.GithubApiClient;
import com.learn.dev_bot.services.github.GithubRateLimiter;
import com.learn.dev_bot.utils.ai.RagSettings;
import com.learn.dev_bot.utils.indexing.CodeChunker;
import com.learn.dev_bot.utils.indexing.CodeFileFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    private static final int VECTOR_BATCH_SIZE = 32;
    private static final int PROGRESS_EVERY_N_FILES = 5;

    private final RepoRepository repoRepository;
    private final UsersService usersService;
    private final GithubApiClient githubApiClient;
    private final CodeFileFilter fileFilter;
    private final CodeChunker codeChunker;
    private final GithubRateLimiter githubRateLimiter;
    private final VectorStore vectorStore;

    @Value("${app.indexing.max-file-bytes}")
    private long maxFileBytes;

    public Repository startIndexing (UUID repoID, UUID userId) {

        Repository repo = repoRepository.findByIdAndUserId(repoID, userId)
                .orElseThrow(() -> new NotFoundException("Repository not found."));

        if (repo.getIndexStatus() == IndexStatus.INDEXING) {
            throw new BadRequestException("Repository is already being indexed");
        }

        repo.setIndexStatus(IndexStatus.INDEXING);
        repo.setFilesProcessed(0);
        repo.setFilesTotal(0);
        repo.setChunkCount(0);
        repo.setErrorMessages(null);
        repo.setUpdatedAt(Instant.now());

        return repoRepository.save(repo);
    }

    @Async("indexingExecutor")
    public void indexAsync(UUID repoId, UUID userId) {
        try {
            doIndex(repoId, userId);
        } catch (Exception ex) {
            log.error("Indexing failed for repo {}", repoId, ex);
            markFailed(repoId, ex.getMessage());
        }
    }

    private void doIndex(UUID repoId, UUID userId) {
        Repository repo = repoRepository.findById(repoId)
                .orElseThrow(() -> new NotFoundException("Repository not found."));

        String token = usersService.decryptAccessToken(usersService.requiredById(userId));

        deleteExistingVectors(repoId.toString());

        Map<String, Object> tree = githubApiClient.getRepoTree(
                token, repo.getOwner(), repo.getName(), repo.getDefaultBranch());
        List<String> filePaths = listIndexableFiles(tree);

        updateProgress(repoId, filePaths.size(), 0, 0, IndexStatus.INDEXING, null);

        List<Document> batch = new ArrayList<>();
        int processed = 0;
        int totalChunks = 0;

        for (String path : filePaths) {
            try {
                String content = githubApiClient.getFileContent(token, repo.getOwner(), repo.getName(), path);
                List<Document> chunks = codeChunker.chunkFile(repoId.toString(), path, content);

                batch.addAll(chunks);
                totalChunks += chunks.size();
                if (batch.size() >= VECTOR_BATCH_SIZE) {
                    vectorStore.add(batch);
                    batch.clear();
                }
            } catch (Exception ex) {
                log.warn("Skipping file {} in {} : {}", path, repo.getFullName(), ex.getMessage());
            }

            processed++;

            if (processed % PROGRESS_EVERY_N_FILES == 0 || processed == filePaths.size())
                updateProgress(repoId, filePaths.size(), processed, totalChunks, IndexStatus.INDEXING, null);

            githubRateLimiter.pause();
        }

        if (!batch.isEmpty())
            vectorStore.add(batch);

        markReady(repoId, filePaths.size(), processed, totalChunks, repo.getFullName());
    }

    @SuppressWarnings("unchecked")
    private List<String> listIndexableFiles(Map<String, Object> tree) {
        if (tree == null || tree.get("tree") == null)
            return List.of();

        List<Map<String, Object>> entries = (List<Map<String, Object>>) tree.get("tree");

        return entries.stream()
                .filter(entry -> "blob".equals(String.valueOf(entry.get("type"))))
                .filter(entry -> {
                    String path = String.valueOf(entry.get("path"));
                    long size = entry.get("size") instanceof Number num ? num.longValue() : 0L;
                    return fileFilter.isEligible(path, size, maxFileBytes);
                })
                .map(entry -> String.valueOf(entry.get("path")))
                .toList();
    }

    private void deleteExistingVectors (String repoId) {
        try {
            var filter = new FilterExpressionBuilder().eq(RagSettings.METADATA_REPO_ID, repoId).build();

            vectorStore.delete(filter);
        } catch (Exception ex) {

            log.warn("Could not delete existing vectors for repo {} : {}", repoId, ex.getMessage());
        }
    }

    @Transactional
    protected void updateProgress (
            UUID repoId,
            Integer total,
            Integer processed,
            Integer chunks,
            IndexStatus status,
            String error) {

        repoRepository.findById(repoId).ifPresent(repo -> {
            repo.setFilesTotal(total);
            repo.setFilesProcessed(processed);
            repo.setChunkCount(chunks);
            repo.setIndexStatus(status);
            repo.setErrorMessages(error);
            repo.setUpdatedAt(Instant.now());

            repoRepository.save(repo);
        });
    }

    @Transactional
    protected void markReady (UUID repoId, Integer totalFiles, Integer processedFiles,
            Integer totalChunks, String fullName) {

        repoRepository.findById(repoId).ifPresent(repo -> {
           repo.setIndexStatus(IndexStatus.READY);
           repo.setFilesTotal(totalFiles);
           repo.setFilesProcessed(processedFiles);
           repo.setChunkCount(totalChunks);
           repo.setIndexedAt(Instant.now());
           repo.setUpdatedAt(Instant.now());
           repo.setErrorMessages(null);
           repo.setFullName(fullName);

           repoRepository.save(repo);
        });

        log.info("Indexed {} files ({} chunks) for {}", processedFiles, totalChunks, fullName);
    }

    @Transactional
    protected void markFailed(UUID repoId, String message) {
        repoRepository.findById(repoId).ifPresent(repo -> {
            repo.setIndexStatus(IndexStatus.FAILED);
            repo.setErrorMessages(message != null && message.length() > 2000
                    ? message.substring(0, 2000) : message);
            repo.setUpdatedAt(Instant.now());

            repoRepository.save(repo);
        });
    }
}
