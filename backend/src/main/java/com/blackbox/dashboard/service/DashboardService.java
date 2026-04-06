package com.blackbox.dashboard.service;

import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.repository.ActivityLogRepository;
import com.blackbox.alert.repository.AlertRepository;
import com.blackbox.dashboard.dto.ProjectSummaryResponse;
import com.blackbox.project.entity.Project;
import com.blackbox.project.repository.ProjectMemberRepository;
import com.blackbox.project.repository.ProjectRepository;
import com.blackbox.score.entity.ContributionScore;
import com.blackbox.score.repository.ContributionScoreRepository;
import com.blackbox.task.entity.Task;
import com.blackbox.task.entity.TaskStatus;
import com.blackbox.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final TaskRepository taskRepository;
    private final ContributionScoreRepository scoreRepository;
    private final AlertRepository alertRepository;
    private final ActivityLogRepository activityLogRepository;

    public DashboardService(ProjectRepository projectRepository,
                            ProjectMemberRepository memberRepository,
                            TaskRepository taskRepository,
                            ContributionScoreRepository scoreRepository,
                            AlertRepository alertRepository,
                            ActivityLogRepository activityLogRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.taskRepository = taskRepository;
        this.scoreRepository = scoreRepository;
        this.alertRepository = alertRepository;
        this.activityLogRepository = activityLogRepository;
    }

    public List<ProjectSummaryResponse> getOverview(UUID userId) {
        List<Project> projects = projectRepository.findAllByMemberUserId(userId);

        return projects.stream()
                .map(this::buildSummary)
                .collect(Collectors.toList());
    }

    private ProjectSummaryResponse buildSummary(Project project) {
        UUID pid = project.getId();

        // 멤버 수
        int memberCount = memberRepository.findAllByProjectId(pid).size();

        // 태스크 통계
        List<Task> tasks = taskRepository.findAllByProjectIdOrderByCreatedAtDesc(pid);
        int taskTotal = tasks.size();
        int taskTodo = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        int taskInProgress = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        int taskDone = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

        // 기여도 통계
        List<ContributionScore> scores = scoreRepository.findAllByProjectId(pid);
        BigDecimal scoreAvg = BigDecimal.ZERO;
        BigDecimal scoreMin = BigDecimal.ZERO;
        BigDecimal scoreMax = BigDecimal.ZERO;
        if (!scores.isEmpty()) {
            BigDecimal sum = scores.stream()
                    .map(ContributionScore::getTotalScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            scoreAvg = sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
            scoreMin = scores.stream()
                    .map(ContributionScore::getTotalScore)
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
            scoreMax = scores.stream()
                    .map(ContributionScore::getTotalScore)
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
        }

        // 경보 통계
        long unreadAlertCount = alertRepository.countByProjectIdAndIsReadFalse(pid);
        long totalAlertCount = alertRepository.findAllByProjectIdOrderByCreatedAtDesc(pid).size();

        // 최근 활동 시간
        List<ActivityLog> recentLogs = activityLogRepository.findAllByProjectIdOrderByOccurredAtDesc(pid);
        OffsetDateTime lastActivityAt = recentLogs.isEmpty() ? null : recentLogs.get(0).getOccurredAt();

        // 건강 상태 판정
        String healthStatus = determineHealth(unreadAlertCount, scoreMin, scoreAvg, taskDone, taskTotal);

        return new ProjectSummaryResponse(
                pid,
                project.getName(),
                project.getCourseName(),
                project.getSemester(),
                memberCount,
                taskTotal, taskTodo, taskInProgress, taskDone,
                scoreAvg, scoreMin, scoreMax,
                unreadAlertCount, totalAlertCount,
                healthStatus,
                lastActivityAt
        );
    }

    private String determineHealth(long unreadAlerts, BigDecimal scoreMin,
                                   BigDecimal scoreAvg, int taskDone, int taskTotal) {
        // DANGER: 미확인 경보 3개 이상 또는 최저 점수가 평균의 30% 이하
        if (unreadAlerts >= 3) return "DANGER";
        if (scoreAvg.compareTo(BigDecimal.ZERO) > 0
                && scoreMin.compareTo(scoreAvg.multiply(BigDecimal.valueOf(0.30))) <= 0) {
            return "DANGER";
        }
        // WARNING: 미확인 경보 1개 이상 또는 진행률 30% 미만 (태스크 5개 이상일 때)
        if (unreadAlerts >= 1) return "WARNING";
        if (taskTotal >= 5 && taskDone < taskTotal * 0.3) return "WARNING";
        return "HEALTHY";
    }
}
