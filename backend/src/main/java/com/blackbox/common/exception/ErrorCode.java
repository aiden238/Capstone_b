package com.blackbox.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_001", "이미 사용 중인 이메일입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "이메일 또는 비밀번호가 올바르지 않습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "만료된 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_005", "리프레시 토큰을 찾을 수 없습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다"),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_001", "프로젝트를 찾을 수 없습니다"),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "PROJECT_002", "유효하지 않은 초대 코드입니다"),
    ALREADY_MEMBER(HttpStatus.CONFLICT, "PROJECT_003", "이미 프로젝트 멤버입니다"),
    NOT_A_MEMBER(HttpStatus.FORBIDDEN, "PROJECT_004", "프로젝트 멤버가 아닙니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "PROJECT_005", "이 작업을 수행할 권한이 없습니다"),
    CANNOT_REMOVE_LEADER(HttpStatus.BAD_REQUEST, "PROJECT_006", "리더는 추방할 수 없습니다"),

    // General
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GENERAL_001", "서버 내부 오류가 발생했습니다"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "GENERAL_002", "입력값이 올바르지 않습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
