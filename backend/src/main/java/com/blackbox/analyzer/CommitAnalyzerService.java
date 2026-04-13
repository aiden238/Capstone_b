package com.blackbox.analyzer;

import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 배치 스케줄러: 매일 새벽 2시에 미분석 COMMIT 로그를 Claude API로 분석.
 * - consent_ai_analysis=true 인 멤버만 대상
 * - API Rate Limit 대응: 10건씩 처리 후 1초 대기
 */
@Service
public class CommitAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(CommitAnalyzerService.class);
    private static final int BATCH_SIZE = 10;

    private final ActivityLogRepository activityLogRepository;
    private final ClaudeApiClient claudeApiClient;

    public CommitAnalyzerService(ActivityLogRepository activityLogRepository,
                                 ClaudeApiClient claudeApiClient) {
        this.activityLogRepository = activityLogRepository;
        this.claudeApiClient = claudeApiClient;
    }

    /**
     * 매일 새벽 2시 실행.
     * 미분석 커밋 최대 100건 처리 (10건 단위 배치, 배치 간 1초 대기).
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void analyzeUnanalyzedCommits() {
        log.info("[CommitAnalyzer] 배치 시작");
        int totalProcessed = 0;

        List<ActivityLog> batch;
        do {
            batch = activityLogRepository.findUnanalyzedCommitsWithConsent(
                    PageRequest.of(0, BATCH_SIZE)
            );
            if (batch.isEmpty()) break;

            for (ActivityLog commitLog : batch) {
                processSingle(commitLog);
                totalProcessed++;
            }

            if (batch.size() == BATCH_SIZE) {
                try {
                    Thread.sleep(1000); // Rate limit 대응
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } while (batch.size() == BATCH_SIZE && totalProcessed < 100);

        log.info("[CommitAnalyzer] 배치 완료 — 처리 건수: {}", totalProcessed);
    }

    @Transactional
    public void processSingle(ActivityLog commitLog) {
        ClaudeApiClient.AnalysisResult result = claudeApiClient.analyzeCommit(commitLog.getMetadata());
        if (result == null) {
            // API 미설정 또는 실패 — rule-based 폴백: 기본 1.0 유지
            commitLog.setQualityScore(BigDecimal.ONE);
            commitLog.setQualityReason("rule-based: API unavailable");
            commitLog.setAnalysisMethod("RULE_BASED");
        } else {
            commitLog.setQualityScore(BigDecimal.valueOf(result.score()).setScale(2, java.math.RoundingMode.HALF_UP));
            commitLog.setQualityReason(result.reason());
            commitLog.setAnalysisMethod("AI_CLAUDE");
        }
        activityLogRepository.save(commitLog);
    }
}
