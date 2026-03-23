package com.blackbox.project.repository;

import com.blackbox.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByInviteCode(String inviteCode);

    @Query("SELECT p FROM Project p JOIN ProjectMember pm ON pm.project = p WHERE pm.user.id = :userId ORDER BY p.createdAt DESC")
    List<Project> findAllByMemberUserId(@Param("userId") UUID userId);
}
