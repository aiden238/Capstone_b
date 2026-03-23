package com.blackbox.project.service;

import com.blackbox.auth.entity.User;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.project.dto.*;
import com.blackbox.project.entity.Project;
import com.blackbox.project.entity.ProjectMember;
import com.blackbox.project.entity.ProjectRole;
import com.blackbox.project.repository.ProjectMemberRepository;
import com.blackbox.project.repository.ProjectRepository;
import com.blackbox.project.security.ProjectAccessChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectAccessChecker accessChecker;
    private static final SecureRandom RANDOM = new SecureRandom();

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository memberRepository,
                          UserRepository userRepository,
                          ProjectAccessChecker accessChecker) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.accessChecker = accessChecker;
    }

    @Transactional
    public ProjectResponse create(UUID userId, CreateProjectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Project project = new Project(
                request.getName(),
                request.getDescription(),
                request.getCourseName(),
                request.getSemester(),
                request.getStartDate(),
                request.getEndDate(),
                generateInviteCode(),
                user
        );

        Project saved = projectRepository.save(project);

        // 생성자를 LEADER로 자동 등록
        ProjectMember leader = new ProjectMember(saved, user, ProjectRole.LEADER);
        memberRepository.save(leader);

        return ProjectResponse.from(saved);
    }

    public List<ProjectResponse> getMyProjects(UUID userId) {
        return projectRepository.findAllByMemberUserId(userId).stream()
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProject(UUID projectId, UUID userId) {
        accessChecker.checkAnyMember(projectId, userId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(UUID projectId, UUID userId, UpdateProjectRequest request) {
        accessChecker.checkLeader(projectId, userId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getCourseName() != null) project.setCourseName(request.getCourseName());
        if (request.getSemester() != null) project.setSemester(request.getSemester());
        if (request.getStartDate() != null) project.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) project.setEndDate(request.getEndDate());

        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(UUID projectId, UUID userId) {
        accessChecker.checkLeader(projectId, userId);
        projectRepository.deleteById(projectId);
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return code.toString();
    }
}
