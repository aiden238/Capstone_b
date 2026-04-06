package com.blackbox.meeting.controller;

import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.meeting.dto.*;
import com.blackbox.meeting.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponse>> create(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateMeetingRequest request) {
        MeetingResponse response = meetingService.create(projectId, user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMeetings(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        List<MeetingResponse> response = meetingService.getMeetings(projectId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeeting(
            @PathVariable UUID projectId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal CustomUserDetails user) {
        MeetingResponse response = meetingService.getMeeting(projectId, meetingId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingResponse>> update(
            @PathVariable UUID projectId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UpdateMeetingRequest request) {
        MeetingResponse response = meetingService.update(projectId, meetingId, user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal CustomUserDetails user) {
        meetingService.delete(projectId, meetingId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{meetingId}/checkin")
    public ResponseEntity<ApiResponse<MeetingResponse>> checkin(
            @PathVariable UUID projectId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CheckinRequest request) {
        MeetingResponse response = meetingService.checkin(projectId, meetingId, user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
