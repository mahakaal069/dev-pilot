package com.learn.dev_bot.repository;

import com.learn.dev_bot.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsersRepository extends JpaRepository<Users, UUID> {

    Optional<Users> findByGithubId(long githubId);
}
