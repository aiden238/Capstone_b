package com.blackbox.project.controller;

import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.project.dto.*;
import com.blackbox.project.service.ProjectMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectMemberController {

    private final ProjectMemberService memberService;

    public ProjectMemberController(ProjectMemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<MemberResponse>> join(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody JoinProjectRequest request) {
        MemberResponse response = memberService.joinByInviteCode(user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getMembers(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        List<MemberResponse> response = memberService.getMembers(projectId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{projectId}/members/{memberId}/role")
    public ResponseEntity<ApiResponse<MemberResponse>> updateRole(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        MemberResponse response = memberService.updateRole(projectId, memberId, user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal CustomUserDetails user) {
        memberService.removeMember(projectId, memberId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{projectId}/members/me/consent")
    public ResponseEntity<ApiResponse<MemberResponse>> updateConsent(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ConsentRequest request) {
        MemberResponse response = memberService.updateConsent(projectId, user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{projectId}/invite-code")
    public ResponseEntity<ApiResponse<Map<String, String>>> regenerateInviteCode(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        String newCode = memberService.regenerateInviteCode(projectId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("inviteCode", newCode)));
    }
}
