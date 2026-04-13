package com.blackbox.integration.repository;

import com.blackbox.integration.entity.ExternalAuth;
import com.blackbox.integration.entity.IntegrationProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExternalAuthRepository extends JpaRepository<ExternalAuth, UUID> {

    Optional<ExternalAuth> findByUserIdAndProvider(UUID userId, IntegrationProvider provider);

    boolean existsByUserIdAndProvider(UUID userId, IntegrationProvider provider);
}
