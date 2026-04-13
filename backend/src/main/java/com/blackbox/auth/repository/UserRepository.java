package com.blackbox.auth.repository;

import com.blackbox.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGithubUsername(String githubUsername);

    Optional<User> findByGoogleEmail(String googleEmail);
}
