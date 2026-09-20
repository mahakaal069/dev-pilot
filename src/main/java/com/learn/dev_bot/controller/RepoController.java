package com.learn.dev_bot.controller;

import com.learn.dev_bot.dto.IndexStatusResponse;
import com.learn.dev_bot.dto.RepositoryResponse;
import com.learn.dev_bot.entity.Repository;
import com.learn.dev_bot.security.CurrentUser;
import com.learn.dev_bot.services.IndexingService;
import com.learn.dev_bot.services.RepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repos")
@RequiredArgsConstructor
public class RepoController {

    private final CurrentUser currentUser;
    private final RepoService repoService;
    private final IndexingService indexingService;

    @GetMapping
    public List<RepositoryResponse> list(@RequestParam(name = "refresh", defaultValue = "true") boolean refresh) {

        UUID userId = currentUser.require().getUser().getId();

        if(refresh)
            return repoService.syncAndListRepos(userId);

        return repoService.listSorted(userId);
    }

    @GetMapping("/{id}")
    public RepositoryResponse get(@PathVariable UUID id) {
        UUID userId = currentUser.require().getUser().getId();

        return repoService.toResponse(repoService.requiredOwned(id, userId));
    }

    @GetMapping("/{id}/status")
    public IndexStatusResponse status(@PathVariable UUID id) {
        UUID userId = currentUser.require().getUser().getId();

        return repoService.status(id, userId);
    }

    @PostMapping("/{id}/index")
    public ResponseEntity<RepositoryResponse> index(@PathVariable UUID id) {

        UUID userId = currentUser.require().getUser().getId();
        Repository repo = indexingService.startIndexing(id, userId);

        indexingService.indexAsync(id, userId);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(repoService.toResponse(repo));
    }
}
