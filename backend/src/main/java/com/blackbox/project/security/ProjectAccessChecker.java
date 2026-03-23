package com.blackbox.project.security;

import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.project.entity.ProjectMember;
import com.blackbox.project.entity.ProjectRole;
import com.blackbox.project.repository.ProjectMemberRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProjectAccessChecker {

    private final ProjectMemberRepository memberRepository;

    public ProjectAccessChecker(ProjectMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /** LEADER만 허용 */
    public ProjectMember checkLeader(UUID projectId, UUID userId) {
        ProjectMember member = getMember(projectId, userId);
        if (member.getRole() != ProjectRole.LEADER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return member;
    }

    /** LEADER 또는 MEMBER만 허용 (OBSERVER 제외) — INV-04 준수 */
    public ProjectMember checkMemberOrAbove(UUID projectId, UUID userId) {
        ProjectMember member = getMember(projectId, userId);
        if (member.getRole() == ProjectRole.OBSERVER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return member;
    }

    /** 모든 멤버 허용 (OBSERVER 포함) */
    public ProjectMember checkAnyMember(UUID projectId, UUID userId) {
        return getMember(projectId, userId);
    }

    private ProjectMember getMember(UUID projectId, UUID userId) {
        return memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_MEMBER));
    }
}
