package com.blackbox.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API 클라이언트.
 * 커밋 메타데이터를 전송하고 quality_score (0.0~1.5) + 이유를 반환한다.
 */
@Component
public class ClaudeApiClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiClient.class);
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-3-5-haiku-20241022";

    @Value("${claude.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public ClaudeApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * @param commitMetadata JSON string containing commit message and diff stats
     * @return AnalysisResult with score (0.0~1.5) and reason; null if API unavailable
     */
    public AnalysisResult analyzeCommit(String commitMetadata) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("CLAUDE_API_KEY 미설정 — 분석 건너뜀");
            return null;
        }

        String prompt = """
                다음 Git 커밋 정보를 분석하여 코드 기여 품질을 평가해주세요.
                
                평가 기준:
                - 커밋 메시지의 명확성과 구체성
                - 변경 규모 대비 설명의 충분함
                - 의미 있는 변경(기능 추가/버그 수정) vs 단순 변경(타이핑 수정/공백)
                
                점수 범위: 0.0 (무의미/매우 낮음) ~ 1.0 (보통) ~ 1.5 (탁월함)
                
                응답 형식 (JSON만):
                {"score": 0.0~1.5, "reason": "한 줄 설명"}
                
                커밋 정보:
                """ + commitMetadata;

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", 256,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    CLAUDE_API_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody());
            }
        } catch (Exception e) {
            log.warn("Claude API 호출 실패: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private AnalysisResult parseResponse(Map<?, ?> body) {
        try {
            List<?> content = (List<?>) body.get("content");
            if (content == null || content.isEmpty()) return null;

            Map<?, ?> first = (Map<?, ?>) content.get(0);
            String text = (String) first.get("text");
            if (text == null) return null;

            // JSON 파싱 — 간단한 수동 파싱 (외부 의존성 없이)
            text = text.trim();
            int scoreIdx = text.indexOf("\"score\"");
            int reasonIdx = text.indexOf("\"reason\"");
            if (scoreIdx < 0 || reasonIdx < 0) return null;

            // score 파싱
            int colonIdx = text.indexOf(':', scoreIdx);
            int commaIdx = text.indexOf(',', colonIdx);
            String scoreStr = text.substring(colonIdx + 1, commaIdx).trim();
            double score = Double.parseDouble(scoreStr);
            score = Math.max(0.0, Math.min(1.5, score));

            // reason 파싱
            int reasonColonIdx = text.indexOf(':', reasonIdx);
            int reasonStart = text.indexOf('"', reasonColonIdx + 1) + 1;
            int reasonEnd = text.indexOf('"', reasonStart);
            String reason = reasonEnd > reasonStart ? text.substring(reasonStart, reasonEnd) : "";

            return new AnalysisResult(score, reason);
        } catch (Exception e) {
            log.warn("Claude 응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    public record AnalysisResult(double score, String reason) {}
}
