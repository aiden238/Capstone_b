package com.blackbox.integration.repository;

import com.blackbox.integration.entity.ExternalIntegration;
import com.blackbox.integration.entity.IntegrationProvider;
import com.blackbox.integration.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalIntegrationRepository extends JpaRepository<ExternalIntegration, UUID> {

    List<ExternalIntegration> findAllByProjectId(UUID projectId);

    List<ExternalIntegration> findAllByProjectIdAndProvider(UUID projectId, IntegrationProvider provider);

    Optional<ExternalIntegration> findByProjectIdAndProviderAndExternalId(
            UUID projectId, IntegrationProvider provider, String externalId);

    List<ExternalIntegration> findAllByProviderAndSyncStatus(IntegrationProvider provider, SyncStatus syncStatus);

    Optional<ExternalIntegration> findByProviderAndExternalId(IntegrationProvider provider, String externalId);

    List<ExternalIntegration> findAllByInstallationId(Long installationId);
}
