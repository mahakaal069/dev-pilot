package com.learn.dev_bot.security;

import com.learn.dev_bot.entity.Users;
import com.learn.dev_bot.services.UsersService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GithubOauth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UsersService usersService;

    private final DefaultOAuth2UserService defaultOAuth2UserService = new DefaultOAuth2UserService();

    @Override
    public @Nullable OAuth2User loadUser(@NonNull OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User githubUser = defaultOAuth2UserService.loadUser(userRequest);

        String accessToken = userRequest.getAccessToken().getTokenValue();
        String scopes = String.join(",", userRequest.getAccessToken().getScopes());

        Users user = usersService.upsertFromGitHub(githubUser.getAttributes(), accessToken, scopes);

        return new AppUserPrincipal(user, githubUser.getAttributes());
    }
}
