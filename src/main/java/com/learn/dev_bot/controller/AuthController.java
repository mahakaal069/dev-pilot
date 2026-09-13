package com.learn.dev_bot.controller;

import com.learn.dev_bot.dto.UserResponse;
import com.learn.dev_bot.entity.Users;
import com.learn.dev_bot.security.AppUserPrincipal;
import com.learn.dev_bot.security.CurrentUser;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final CurrentUser currentUser;

    @GetMapping("/login-url")
    public Map<String, String> loginUrl() {

        return Map.of("url", "/oauth2/authorization/github");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser() {
        AppUserPrincipal principal = currentUser.require();
        Users user = principal.getUser();

        return ResponseEntity.ok(new UserResponse(
                user.getId(),
                user.getGithubId(),
                user.getGithubUsername(),
                user.getDisplayName(),
                user.getAvatarUrl()
        ));
    }
}
