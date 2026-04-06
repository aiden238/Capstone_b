package com.blackbox.alert.service;

import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.repository.ActivityLogRepository;
import com.blackbox.alert.dto.AlertResponse;
import com.blackbox.alert.entity.Alert;
import com.blackbox.alert.entity.AlertSeverity;
import com.blackbox.alert.entity.AlertType;
import com.blackbox.alert.repository.AlertRepository;
import com.blackbox.project.entity.ProjectMember;
import com.blackbox.project.entity.ProjectRole;
import com.blackbox.project.repository.ProjectMemberRepository;
import com.blackbox.score.entity.ContributionScore;
import com.blackbox.score.repository.ContributionScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private static final BigDecimal IMBALANCE_THRESHOLD = new BigDecimal("40");   // 평균 대비 40% 이하
    private static final BigDecimal OVERLOAD_THRESHOLD = new BigDecimal("60");     // 전체의 60% 이상
    private static final int DROPOUT_DAYS = 14;                                    // 2주 무활동
    private static final int GAMING_WINDOW_MINUTES = 30;                           // 30분 내
    private static final int GAMING_ACTION_THRESHOLD = 15;                         // 15건 이상

    private final AlertRepository alertRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ProjectMemberRepository memberRepository;
    private final ContributionScoreRepository scoreRepository;

    public AlertService(AlertRepository alertRepository,
                        ActivityLogRepository activityLogRepository,
                        ProjectMemberRepository memberRepository,
                        ContributionScoreRepository scoreRepository) {
        this.alertRepository = alertRepository;
        this.activityLogRepository = activityLogRepository;
        this.memberRepository = memberRepository;
        this.scoreRepository = scoreRepository;
    }

    /**
     * 프로젝트의 모든 경보 조회
     */
    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts(UUID projectId) {
        return alertRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * 읽지 않은 경보만 조회
     */
    @Transactional(readOnly = true)
    public List<AlertResponse> getUnreadAlerts(UUID projectId) {
        return alertRepository.findAllByProjectIdAndIsReadFalseOrderByCreatedAtDesc(projectId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * 읽지 않은 경보 수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID projectId) {
        return alertRepository.countByProjectIdAndIsReadFalse(projectId);
    }

    /**
     * 경보 읽음 처리
     */
    @Transactional
    public void markAsRead(UUID alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setIsRead(true);
            alertRepository.save(alert);
        });
    }

    /**
     * 전체 읽음 처리
     */
    @Transactional
    public void markAllAsRead(UUID projectId) {
        List<Alert> unread = alertRepository.findAllByProjectIdAndIsReadFalseOrderByCreatedAtDesc(projectId);
        unread.forEach(a -> a.setIsRead(true));
        alertRepository.saveAll(unread);
    }

    /**
     * 경보 규칙 실행 (수동 트리거 또는 점수 계산 후 호출)
     */
    @Transactional
    public List<AlertResponse> runDetection(UUID projectId) {
        List<AlertResponse> newAlerts = new ArrayList<>();

        newAlerts.addAll(detectFreeRide(projectId));
        newAlerts.addAll(detectOverload(projectId));
        newAlerts.addAll(detectDropout(projectId));
        newAlerts.addAll(detectGaming(projectId));

        return newAlerts;
    }

    /**
     * FREE_RIDE: 기여도가 팀 평균의 40% 이하
     */
    private List<AlertResponse> detectFreeRide(UUID projectId) {
        List<AlertResponse> results = new ArrayList<>();
        List<ContributionScore> scores = scoreRepository.findAllByProjectId(projectId);

        if (scores.size() < 2) return results;

        BigDecimal avg = scores.stream()
                .map(ContributionScore::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(scores.size()), 2, RoundingMode.HALF_UP);

        if (avg.compareTo(BigDecimal.ZERO) == 0) return results;

        BigDecimal threshold = avg.multiply(IMBALANCE_THRESHOLD).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        for (ContributionScore score : scores) {
            if (score.getTotalScore().compareTo(threshold) <= 0 && score.getTotalScore().compareTo(BigDecimal.ZERO) >= 0) {
                String userName = score.getUser().getName();
                Alert alert = new Alert(projectId, score.getUser().getId(),
                        AlertType.FREE_RIDE, AlertSeverity.HIGH,
                        String.format("%s님의 기여도(%.1f)가 팀 평균(%.1f)의 40%% 이하입니다.",
                                userName, score.getTotalScore(), avg));
                alertRepository.save(alert);
                results.add(toResponse(alert));
            }
        }
        return results;
    }

    /**
     * OVERLOAD: 한 멤버가 전체 활동의 60% 이상 차지
     */
    private List<AlertResponse> detectOverload(UUID projectId) {
        List<AlertResponse> results = new ArrayList<>();
        List<ActivityLog> logs = activityLogRepository.findAllByProjectIdOrderByOccurredAtDesc(projectId);

        if (logs.size() < 5) return results; // 의미 있는 판단을 위한 최소 활동 수

        Map<UUID, Long> countByUser = logs.stream()
                .collect(Collectors.groupingBy(ActivityLog::getUserId, Collectors.counting()));

        long total = logs.size();
        for (Map.Entry<UUID, Long> entry : countByUser.entrySet()) {
            BigDecimal ratio = new BigDecimal(entry.getValue())
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);

            if (ratio.compareTo(OVERLOAD_THRESHOLD) >= 0) {
                Alert alert = new Alert(projectId, entry.getKey(),
                        AlertType.OVERLOAD, AlertSeverity.MEDIUM,
                        String.format("한 팀원이 전체 활동의 %.1f%%를 수행하고 있습니다. 업무 분배를 확인하세요.", ratio));
                alertRepository.save(alert);
                results.add(toResponse(alert));
            }
        }
        return results;
    }

    /**
     * DROPOUT: 2주 이상 활동 없음
     */
    private List<AlertResponse> detectDropout(UUID projectId) {
        List<AlertResponse> results = new ArrayList<>();
        List<ProjectMember> members = memberRepository.findAllByProjectId(projectId).stream()
                .filter(m -> m.getRole() != ProjectRole.OBSERVER)
                .toList();

        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(DROPOUT_DAYS);

        for (ProjectMember m : members) {
            List<ActivityLog> userLogs = activityLogRepository
                    .findAllByProjectIdAndUserIdOrderByOccurredAtDesc(projectId, m.getUser().getId());

            boolean hasRecentActivity = userLogs.stream()
                    .anyMatch(log -> log.getOccurredAt().isAfter(cutoff));

            if (!hasRecentActivity && !userLogs.isEmpty()) {
                // 활동 이력이 있지만 최근 2주간 없음
                Alert alert = new Alert(projectId, m.getUser().getId(),
                        AlertType.DROPOUT, AlertSeverity.HIGH,
                        String.format("%s님이 %d일 이상 활동이 없습니다.", m.getUser().getName(), DROPOUT_DAYS));
                alertRepository.save(alert);
                results.add(toResponse(alert));
            }
        }
        return results;
    }

    /**
     * GAMING_SUSPECT: 짧은 시간에 비정상적 대량 활동
     */
    private List<AlertResponse> detectGaming(UUID projectId) {
        List<AlertResponse> results = new ArrayList<>();
        List<ProjectMember> members = memberRepository.findAllByProjectId(projectId).stream()
                .filter(m -> m.getRole() != ProjectRole.OBSERVER)
                .toList();

        for (ProjectMember m : members) {
            List<ActivityLog> userLogs = activityLogRepository
                    .findAllByProjectIdAndUserIdOrderByOccurredAtDesc(projectId, m.getUser().getId());

            if (userLogs.size() < GAMING_ACTION_THRESHOLD) continue;

            // 최근 30분 내 활동 수 확인
            OffsetDateTime windowStart = OffsetDateTime.now().minusMinutes(GAMING_WINDOW_MINUTES);
            long recentCount = userLogs.stream()
                    .filter(log -> log.getOccurredAt().isAfter(windowStart))
                    .count();

            if (recentCount >= GAMING_ACTION_THRESHOLD) {
                Alert alert = new Alert(projectId, m.getUser().getId(),
                        AlertType.GAMING_SUSPECT, AlertSeverity.CRITICAL,
                        String.format("%s님이 %d분 내 %d건의 활동을 수행했습니다. 점수 조작이 의심됩니다.",
                                m.getUser().getName(), GAMING_WINDOW_MINUTES, recentCount));
                alertRepository.save(alert);
                results.add(toResponse(alert));
            }
        }
        return results;
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(), alert.getProjectId(), alert.getUserId(),
                alert.getAlertType(), alert.getSeverity(), alert.getMessage(),
                alert.getIsRead(), alert.getCreatedAt()
        );
    }
}
