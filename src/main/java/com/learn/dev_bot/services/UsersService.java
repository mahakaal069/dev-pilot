package com.learn.dev_bot.services;

import com.learn.dev_bot.entity.Users;
import com.learn.dev_bot.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersService {
    private final UsersRepository userRepository;
    private final TextEncryptor textEncryptor;

    @Transactional
    public Users upsertFromGitHub(@NonNull Map<String, Object> attributes, String accessToken, String scopes) {
        Long githubId = toLong(attributes.get("id"));
        String login = String.valueOf(attributes.get("login"));
        String name = attributes.get("name") != null
                ? String.valueOf(attributes.get("name"))
                : login;
        String avatarUrl = attributes.get("avatar_url") != null
                ? String.valueOf(attributes.get("avatar_url"))
                : null;

        String encryptedToken = textEncryptor.encrypt(accessToken);

        Users user = userRepository.findByGithubId(githubId).orElseGet(Users::new);
        user.setGithubId(githubId);
        user.setGithubUsername(login);
        user.setDisplayName(name);
        user.setAvatarUrl(avatarUrl);
        user.setAccessToken(encryptedToken);
        user.setTokenScope(scopes);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Users requiredById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found in the database."));
    }

    public String decryptAccessToken(@NonNull Users user) {
        return textEncryptor.decrypt(user.getAccessToken());
    }

    public static Long toLong(Object value) {
        if(value instanceof Number number)
            return number.longValue();

        return Long.parseLong(String.valueOf(value));
    }
}
