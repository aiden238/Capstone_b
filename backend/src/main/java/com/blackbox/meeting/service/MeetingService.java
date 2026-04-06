package com.blackbox.meeting.service;

import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.service.ActivityLogService;
import com.blackbox.auth.entity.User;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.meeting.dto.*;
import com.blackbox.meeting.entity.Meeting;
import com.blackbox.meeting.entity.MeetingAttendee;
import com.blackbox.meeting.repository.MeetingAttendeeRepository;
import com.blackbox.meeting.repository.MeetingRepository;
import com.blackbox.project.entity.Project;
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
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingAttendeeRepository attendeeRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessChecker accessChecker;
    private final ActivityLogService activityLogService;
    private static final SecureRandom RANDOM = new SecureRandom();

    public MeetingService(MeetingRepository meetingRepository,
                          MeetingAttendeeRepository attendeeRepository,
                          ProjectRepository projectRepository,
                          UserRepository userRepository,
                          ProjectAccessChecker accessChecker,
                          ActivityLogService activityLogService) {
        this.meetingRepository = meetingRepository;
        this.attendeeRepository = attendeeRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.accessChecker = accessChecker;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public MeetingResponse create(UUID projectId, UUID userId, CreateMeetingRequest request) {
        accessChecker.checkMemberOrAbove(projectId, userId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Meeting meeting = new Meeting(
                project,
                request.getTitle(),
                request.getMeetingDate(),
                request.getPurpose(),
                generateCheckinCode(),
                creator
        );

        Meeting saved = meetingRepository.save(meeting);

        activityLogService.log(projectId, userId, ActionType.MEETING_CREATE,
                "{\"meetingId\":\"" + saved.getId() + "\",\"title\":\"" + saved.getTitle() + "\"}");

        return MeetingResponse.from(saved);
    }

    public List<MeetingResponse> getMeetings(UUID projectId, UUID userId) {
        accessChecker.checkAnyMember(projectId, userId);
        return meetingRepository.findAllByProjectIdOrderByMeetingDateDesc(projectId).stream()
                .map(MeetingResponse::from)
                .collect(Collectors.toList());
    }

    public MeetingResponse getMeeting(UUID projectId, UUID meetingId, UUID userId) {
        accessChecker.checkAnyMember(projectId, userId);
        Meeting meeting = findMeetingInProject(projectId, meetingId);
        return MeetingResponse.from(meeting);
    }

    @Transactional
    public MeetingResponse update(UUID projectId, UUID meetingId, UUID userId, UpdateMeetingRequest request) {
        accessChecker.checkMemberOrAbove(projectId, userId);
        Meeting meeting = findMeetingInProject(projectId, meetingId);

        if (request.getTitle() != null) meeting.setTitle(request.getTitle());
        if (request.getPurpose() != null) meeting.setPurpose(request.getPurpose());
        if (request.getNotes() != null) meeting.setNotes(request.getNotes());
        if (request.getDecisions() != null) meeting.setDecisions(request.getDecisions());

        activityLogService.log(projectId, userId, ActionType.MEETING_UPDATE,
                "{\"meetingId\":\"" + meeting.getId() + "\"}");

        return MeetingResponse.from(meeting);
    }

    @Transactional
    public void delete(UUID projectId, UUID meetingId, UUID userId) {
        accessChecker.checkMemberOrAbove(projectId, userId);
        Meeting meeting = findMeetingInProject(projectId, meetingId);

        activityLogService.log(projectId, userId, ActionType.MEETING_DELETE,
                "{\"meetingId\":\"" + meeting.getId() + "\",\"title\":\"" + meeting.getTitle() + "\"}");

        meetingRepository.delete(meeting);
    }

    @Transactional
    public MeetingResponse checkin(UUID projectId, UUID meetingId, UUID userId, CheckinRequest request) {
        accessChecker.checkAnyMember(projectId, userId);
        Meeting meeting = findMeetingInProject(projectId, meetingId);

        if (!meeting.getCheckinCode().equals(request.getCheckinCode())) {
            throw new BusinessException(ErrorCode.INVALID_CHECKIN_CODE);
        }

        // 이미 체크인했는지 확인
        var existing = attendeeRepository.findByMeetingIdAndUserId(meetingId, userId);
        if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getCheckedIn())) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);
        }

        if (existing.isPresent()) {
            MeetingAttendee attendee = existing.get();
            attendee.setCheckedIn(true);
            attendee.setCheckedAt(OffsetDateTime.now());
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            MeetingAttendee attendee = new MeetingAttendee(meeting, user);
            attendee.setCheckedIn(true);
            attendee.setCheckedAt(OffsetDateTime.now());
            meeting.getAttendees().add(attendee);
        }

        activityLogService.log(projectId, userId, ActionType.MEETING_CHECKIN,
                "{\"meetingId\":\"" + meeting.getId() + "\"}");

        return MeetingResponse.from(meeting);
    }

    private Meeting findMeetingInProject(UUID projectId, UUID meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
        if (!meeting.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }
        return meeting;
    }

    private String generateCheckinCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return code.toString();
    }
}
