package com.blackbox.collector.github.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * GitHub Webhook X-Hub-Signature-256 검증.
 * HMAC-SHA256(secret, payload) 비교 — 시간 상수 비교(MessageDigest.isEqual)로 타이밍 공격 방지.
 */
@Component
public class GitHubWebhookVerifier {

    private final String webhookSecret;

    public GitHubWebhookVerifier(
            @Value("${github.webhook-secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    /**
     * @param payloadBytes  raw request body
     * @param signatureHeader  X-Hub-Signature-256 헤더값 (e.g. "sha256=abc123...")
     * @return true if valid or webhook secret is not configured (dev mode)
     */
    public boolean verify(byte[] payloadBytes, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true; // 개발 환경: 시크릿 미설정 시 검증 스킵
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payloadBytes);
            String computedHex = "sha256=" + bytesToHex(computed);
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
