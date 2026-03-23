package com.blackbox.project.dto;

import com.blackbox.project.entity.ProjectMember;
import com.blackbox.project.entity.ProjectRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public class MemberResponse {

    private UUID memberId;
    private UUID userId;
    private String email;
    private String name;
    private String avatarUrl;
    private ProjectRole role;
    private OffsetDateTime joinedAt;
    private Boolean consentPlatform;
    private Boolean consentGithub;
    private Boolean consentDrive;
    private Boolean consentAiAnalysis;
    private OffsetDateTime consentedAt;

    public static MemberResponse from(ProjectMember member) {
        MemberResponse r = new MemberResponse();
        r.memberId = member.getId();
        r.userId = member.getUser().getId();
        r.email = member.getUser().getEmail();
        r.name = member.getUser().getName();
        r.avatarUrl = member.getUser().getAvatarUrl();
        r.role = member.getRole();
        r.joinedAt = member.getJoinedAt();
        r.consentPlatform = member.getConsentPlatform();
        r.consentGithub = member.getConsentGithub();
        r.consentDrive = member.getConsentDrive();
        r.consentAiAnalysis = member.getConsentAiAnalysis();
        r.consentedAt = member.getConsentedAt();
        return r;
    }

    // Getters
    public UUID getMemberId() { return memberId; }
    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getAvatarUrl() { return avatarUrl; }
    public ProjectRole getRole() { return role; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
    public Boolean getConsentPlatform() { return consentPlatform; }
    public Boolean getConsentGithub() { return consentGithub; }
    public Boolean getConsentDrive() { return consentDrive; }
    public Boolean getConsentAiAnalysis() { return consentAiAnalysis; }
    public OffsetDateTime getConsentedAt() { return consentedAt; }
}
