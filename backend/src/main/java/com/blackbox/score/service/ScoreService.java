package com.blackbox.score.service;

import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.repository.ActivityLogRepository;
import com.blackbox.auth.entity.User;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.project.entity.ProjectMember;
import com.blackbox.project.entity.ProjectRole;
import com.blackbox.project.repository.ProjectMemberRepository;
import com.blackbox.score.dto.ScoreResponse;
import com.blackbox.score.entity.ContributionScore;
import com.blackbox.score.entity.WeightConfig;
import com.blackbox.score.repository.ContributionScoreRepository;
import com.blackbox.score.repository.WeightConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoreService {

    private static final BigDecimal BASELINE = new BigDecimal("100.00");
    private static final BigDecimal CAP = new BigDecimal("150.00");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // Action type -> category mapping
    private static final Set<ActionType> TASK_ACTIONS = Set.of(
            ActionType.TASK_CREATE, ActionType.TASK_UPDATE, ActionType.TASK_STATUS_CHANGE,
            ActionType.TASK_COMPLETE, ActionType.TASK_ASSIGN
    );
    private static final Set<ActionType> MEETING_ACTIONS = Set.of(
            ActionType.MEETING_CREATE, ActionType.MEETING_CHECKIN
    );
    private static final Set<ActionType> DOC_ACTIONS = Set.of(
            ActionType.FILE_UPLOAD, ActionType.DOC_EDIT, ActionType.DOC_CREATE, ActionType.DOC_COMMENT
    );
    private static final Set<ActionType> GIT_ACTIONS = Set.of(
            ActionType.COMMIT, ActionType.PR_OPEN, ActionType.PR_MERGE,
            ActionType.ISSUE_CREATE, ActionType.ISSUE_CLOSE, ActionType.CODE_REVIEW
    );

    // Weights for different action types within a category
    private static final Map<ActionType, BigDecimal> ACTION_WEIGHTS = Map.ofEntries(
            Map.entry(ActionType.TASK_CREATE, new BigDecimal("1.0")),
            Map.entry(ActionType.TASK_UPDATE, new BigDecimal("0.5")),
            Map.entry(ActionType.TASK_STATUS_CHANGE, new BigDecimal("0.5")),
            Map.entry(ActionType.TASK_COMPLETE, new BigDecimal("2.0")),
            Map.entry(ActionType.TASK_ASSIGN, new BigDecimal("0.3")),
            Map.entry(ActionType.MEETING_CREATE, new BigDecimal("1.5")),
            Map.entry(ActionType.MEETING_CHECKIN, new BigDecimal("2.0")),
            Map.entry(ActionType.FILE_UPLOAD, new BigDecimal("1.5")),
            Map.entry(ActionType.DOC_EDIT, new BigDecimal("1.0")),
            Map.entry(ActionType.DOC_CREATE, new BigDecimal("1.5")),
            Map.entry(ActionType.DOC_COMMENT, new BigDecimal("0.5")),
            Map.entry(ActionType.COMMIT, new BigDecimal("1.5")),
            Map.entry(ActionType.PR_OPEN, new BigDecimal("2.0")),
            Map.entry(ActionType.PR_MERGE, new BigDecimal("2.5")),
            Map.entry(ActionType.ISSUE_CREATE, new BigDecimal("1.0")),
            Map.entry(ActionType.ISSUE_CLOSE, new BigDecimal("1.5")),
            Map.entry(ActionType.CODE_REVIEW, new BigDecimal("2.0"))
    );

    private final ActivityLogRepository activityLogRepository;
    private final ProjectMemberRepository memberRepository;
    private final ContributionScoreRepository scoreRepository;
    private final WeightConfigRepository weightConfigRepository;
    private final UserRepository userRepository;

    public ScoreService(ActivityLogRepository activityLogRepository,
                        ProjectMemberRepository memberRepository,
                        ContributionScoreRepository scoreRepository,
                        WeightConfigRepository weightConfigRepository,
                        UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.memberRepository = memberRepository;
        this.scoreRepository = scoreRepository;
        this.weightConfigRepository = weightConfigRepository;
        this.userRepository = userRepository;
    }

    /**
     * 프로젝트의 모든 팀원 점수를 재계산하고 저장
     */
    @Transactional
    public List<ScoreResponse> calculateAndSave(UUID projectId) {
        // 1. 팀원 목록 (OBSERVER 제외 — 학생만 점수 산출)
        List<ProjectMember> members = memberRepository.findAllByProjectId(projectId).stream()
                .filter(m -> m.getRole() != ProjectRole.OBSERVER)
                .toList();

        if (members.isEmpty()) {
            return List.of();
        }

        // 2. 프로젝트의 전체 활동 로그
        List<ActivityLog> allLogs = activityLogRepository.findAllByProjectIdOrderByOccurredAtDesc(projectId);

        // 3. 유저별 카테고리별 raw 점수 계산
        Map<UUID, BigDecimal> taskRaw = new HashMap<>();
        Map<UUID, BigDecimal> meetingRaw = new HashMap<>();
        Map<UUID, BigDecimal> docRaw = new HashMap<>();
        Map<UUID, BigDecimal> gitRaw = new HashMap<>();

        for (ProjectMember m : members) {
            UUID uid = m.getUser().getId();
            taskRaw.put(uid, ZERO);
            meetingRaw.put(uid, ZERO);
            docRaw.put(uid, ZERO);
            gitRaw.put(uid, ZERO);
        }

        for (ActivityLog log : allLogs) {
            UUID uid = log.getUserId();
            if (!taskRaw.containsKey(uid)) continue; // OBSERVER의 로그는 무시

            ActionType at = log.getActionType();
            BigDecimal w = ACTION_WEIGHTS.getOrDefault(at, BigDecimal.ONE);
            BigDecimal trustMultiplier = log.getTrustLevel() != null ? log.getTrustLevel() : BigDecimal.ONE;
            // AI quality_score (0.0–1.5) adjusts raw points when available; default 1.0
            BigDecimal qualityMultiplier = log.getQualityScore() != null ? log.getQualityScore() : BigDecimal.ONE;
            BigDecimal points = w.multiply(trustMultiplier).multiply(qualityMultiplier);

            if (TASK_ACTIONS.contains(at)) {
                taskRaw.merge(uid, points, BigDecimal::add);
            } else if (MEETING_ACTIONS.contains(at)) {
                meetingRaw.merge(uid, points, BigDecimal::add);
            } else if (DOC_ACTIONS.contains(at)) {
                docRaw.merge(uid, points, BigDecimal::add);
            } else if (GIT_ACTIONS.contains(at)) {
                gitRaw.merge(uid, points, BigDecimal::add);
            }
        }

        // 4. 팀 평균 기준 정규화 (평균=100, 상한=150)
        Map<UUID, BigDecimal> taskNorm = normalize(taskRaw);
        Map<UUID, BigDecimal> meetingNorm = normalize(meetingRaw);
        Map<UUID, BigDecimal> docNorm = normalize(docRaw);
        Map<UUID, BigDecimal> gitNorm = normalize(gitRaw);

        // 5. 가중치 로드
        WeightConfig weights = weightConfigRepository.findByProjectId(projectId)
                .orElse(null);
        BigDecimal wGit = weights != null ? weights.getWeightGit() : new BigDecimal("0.30");
        BigDecimal wDoc = weights != null ? weights.getWeightDoc() : new BigDecimal("0.25");
        BigDecimal wMeeting = weights != null ? weights.getWeightMeeting() : new BigDecimal("0.20");
        BigDecimal wTask = weights != null ? weights.getWeightTask() : new BigDecimal("0.25");

        // 6. 종합 점수 = 카테고리별 정규화 점수 × 가중치 합산
        List<ScoreResponse> results = new ArrayList<>();

        for (ProjectMember m : members) {
            UUID uid = m.getUser().getId();
            User user = m.getUser();

            BigDecimal ts = taskNorm.getOrDefault(uid, ZERO);
            BigDecimal ms = meetingNorm.getOrDefault(uid, ZERO);
            BigDecimal ds = docNorm.getOrDefault(uid, ZERO);
            BigDecimal gs = gitNorm.getOrDefault(uid, ZERO);

            BigDecimal total = ts.multiply(wTask)
                    .add(ms.multiply(wMeeting))
                    .add(ds.multiply(wDoc))
                    .add(gs.multiply(wGit))
                    .setScale(2, RoundingMode.HALF_UP);

            // Cap total at 150
            if (total.compareTo(CAP) > 0) {
                total = CAP;
            }

            // Save or update
            ContributionScore score = scoreRepository.findByProjectIdAndUserId(projectId, uid)
                    .orElse(new ContributionScore(m.getProject(), user));

            score.setTaskScore(ts);
            score.setMeetingScore(ms);
            score.setDocScore(ds);
            score.setGitScore(gs);
            score.setTotalScore(total);
            score.setWeightGit(wGit);
            score.setWeightDoc(wDoc);
            score.setWeightMeeting(wMeeting);
            score.setWeightTask(wTask);
            score.setCalculatedAt(OffsetDateTime.now());

            scoreRepository.save(score);

            results.add(new ScoreResponse(
                    uid, user.getName(), user.getEmail(),
                    ts, ms, ds, gs, total,
                    score.getCalculatedAt()
            ));
        }

        return results;
    }

    /**
     * 저장된 점수 조회 (재계산하지 않음)
     */
    @Transactional(readOnly = true)
    public List<ScoreResponse> getScores(UUID projectId) {
        List<ContributionScore> scores = scoreRepository.findAllByProjectId(projectId);

        if (scores.isEmpty()) {
            // 점수가 없으면 계산 후 반환
            return calculateAndSave(projectId);
        }

        return scores.stream().map(s -> {
            User user = s.getUser();
            return new ScoreResponse(
                    user.getId(), user.getName(), user.getEmail(),
                    s.getTaskScore(), s.getMeetingScore(), s.getDocScore(), s.getGitScore(),
                    s.getTotalScore(), s.getCalculatedAt()
            );
        }).toList();
    }

    /**
     * 특정 유저의 점수 조회
     */
    @Transactional(readOnly = true)
    public ScoreResponse getMyScore(UUID projectId, UUID userId) {
        ContributionScore score = scoreRepository.findByProjectIdAndUserId(projectId, userId)
                .orElse(null);

        if (score == null) {
            // 점수가 없으면 전체 계산 후 본인 것 반환
            List<ScoreResponse> all = calculateAndSave(projectId);
            return all.stream()
                    .filter(s -> s.userId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_MEMBER));
        }

        User user = score.getUser();
        return new ScoreResponse(
                user.getId(), user.getName(), user.getEmail(),
                score.getTaskScore(), score.getMeetingScore(), score.getDocScore(), score.getGitScore(),
                score.getTotalScore(), score.getCalculatedAt()
        );
    }

    /**
     * 팀 평균 기준 정규화: 평균=100, 상한=150
     * 활동이 전혀 없으면 모두 0점 반환
     */
    private Map<UUID, BigDecimal> normalize(Map<UUID, BigDecimal> rawScores) {
        Map<UUID, BigDecimal> result = new HashMap<>();

        BigDecimal sum = rawScores.values().stream()
                .reduce(ZERO, BigDecimal::add);

        if (sum.compareTo(ZERO) == 0) {
            // 활동 없음 → 모두 0점
            rawScores.keySet().forEach(uid -> result.put(uid, ZERO));
            return result;
        }

        BigDecimal count = new BigDecimal(rawScores.size());
        BigDecimal avg = sum.divide(count, 10, RoundingMode.HALF_UP);

        for (Map.Entry<UUID, BigDecimal> entry : rawScores.entrySet()) {
            BigDecimal raw = entry.getValue();
            // normalized = (raw / avg) * 100, capped at 150
            BigDecimal normalized = raw.divide(avg, 10, RoundingMode.HALF_UP)
                    .multiply(BASELINE)
                    .setScale(2, RoundingMode.HALF_UP);

            if (normalized.compareTo(CAP) > 0) {
                normalized = CAP;
            }

            result.put(entry.getKey(), normalized);
        }

        return result;
    }
}
