package com.blackbox.integration.service;

import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.entity.ActivitySource;
import com.blackbox.activity.repository.ActivityLogRepository;
import com.blackbox.auth.entity.User;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.integration.dto.TimelineResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TimelineService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public TimelineService(ActivityLogRepository activityLogRepository,
                            UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * 프로젝트 타임라인 조회 (전체 소스 통합, 페이지네이션)
     */
    @Transactional(readOnly = true)
    public List<TimelineResponse> getTimeline(UUID projectId, UUID userId,
                                               String source, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ActivityLog> logs;

        if (userId != null && source != null) {
            ActivitySource src = ActivitySource.valueOf(source);
            logs = activityLogRepository.findAllByProjectIdAndUserIdAndSourceOrderByOccurredAtDesc(
                    projectId, userId, src, pageable);
        } else if (userId != null) {
            logs = activityLogRepository.findAllByProjectIdAndUserIdOrderByOccurredAtDesc(
                    projectId, userId, pageable);
        } else if (source != null) {
            ActivitySource src = ActivitySource.valueOf(source);
            logs = activityLogRepository.findAllByProjectIdAndSourceOrderByOccurredAtDesc(
                    projectId, src, pageable);
        } else {
            logs = activityLogRepository.findAllByProjectIdOrderByOccurredAtDesc(projectId, pageable);
        }

        // 유저 이름 일괄 조회
        List<UUID> userIds = logs.stream().map(ActivityLog::getUserId).distinct().toList();
        Map<UUID, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return logs.stream()
                .map(log -> TimelineResponse.from(log, userNames.getOrDefault(log.getUserId(), "Unknown")))
                .toList();
    }
}
