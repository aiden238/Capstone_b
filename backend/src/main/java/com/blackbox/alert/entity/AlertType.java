package com.blackbox.alert.entity;

public enum AlertType {
    CRUNCH_TIME,      // 마감 직전 대량 작업
    FREE_RIDE,        // 기여도 불균형 (무임승차)
    DROPOUT,          // 2주 이상 활동 없음 (이탈)
    OVERLOAD,         // 한 명이 60% 이상 기여
    TAMPER,           // 파일 변조 감지
    GAMING_SUSPECT    // 점수 조작 의심 (짧은 시간 대량 활동)
}
