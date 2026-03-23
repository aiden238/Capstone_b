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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectAccessChecker accessChecker;
    private static final SecureRandom RANDOM = new SecureRandom();

    public ProjectMemberService(ProjectRepository projectRepository,
                                ProjectMemberRepository memberRepository,
                                UserRepository userRepository,
                                ProjectAccessChecker accessChecker) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.accessChecker = accessChecker;
    }

    @Transactional
    public MemberResponse joinByInviteCode(UUID userId, JoinProjectRequest request) {
        Project project = projectRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));

        if (memberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            throw new BusinessException(ErrorCode.ALREADY_MEMBER);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ProjectMember member = new ProjectMember(project, user, ProjectRole.MEMBER);
        memberRepository.save(member);

        return MemberResponse.from(member);
    }

    public List<MemberResponse> getMembers(UUID projectId, UUID userId) {
        accessChecker.checkAnyMember(projectId, userId);
        return memberRepository.findAllByProjectId(projectId).stream()
                .map(MemberResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MemberResponse updateRole(UUID projectId, UUID memberId, UUID userId, UpdateMemberRoleRequest request) {
        accessChecker.checkLeader(projectId, userId);

        ProjectMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_MEMBER));

        if (!target.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.NOT_A_MEMBER);
        }

        target.setRole(request.getRole());
        return MemberResponse.from(target);
    }

    @Transactional
    public void removeMember(UUID projectId, UUID memberId, UUID userId) {
        ProjectMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_MEMBER));

        if (!target.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.NOT_A_MEMBER);
        }

        // 리더는 추방 불가
        if (target.getRole() == ProjectRole.LEADER) {
            throw new BusinessException(ErrorCode.CANNOT_REMOVE_LEADER);
        }

        // 본인이 탈퇴하거나, 리더가 추방
        boolean isSelf = target.getUser().getId().equals(userId);
        if (!isSelf) {
            accessChecker.checkLeader(projectId, userId);
        }

        memberRepository.delete(target);
    }

    @Transactional
    public MemberResponse updateConsent(UUID projectId, UUID userId, ConsentRequest request) {
        ProjectMember member = accessChecker.checkAnyMember(projectId, userId);

        if (request.getConsentPlatform() != null) member.setConsentPlatform(request.getConsentPlatform());
        if (request.getConsentGithub() != null) member.setConsentGithub(request.getConsentGithub());
        if (request.getConsentDrive() != null) member.setConsentDrive(request.getConsentDrive());
        if (request.getConsentAiAnalysis() != null) member.setConsentAiAnalysis(request.getConsentAiAnalysis());
        member.setConsentedAt(OffsetDateTime.now());

        return MemberResponse.from(member);
    }

    @Transactional
    public String regenerateInviteCode(UUID projectId, UUID userId) {
        accessChecker.checkLeader(projectId, userId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        String newCode = generateInviteCode();
        project.setInviteCode(newCode);

        return newCode;
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
