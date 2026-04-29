package com.company.saju.common.exception;

public enum ErrorCode {
    // Common
    INVALID_INPUT("C001", "잘못된 입력입니다"),
    RESOURCE_NOT_FOUND("C002", "리소스를 찾을 수 없습니다"),
    FORBIDDEN("C003", "접근 권한이 없습니다"),
    INTERNAL_ERROR("C004", "내부 서버 오류가 발생했습니다"),
    CONCURRENT_MODIFICATION("C005", "동시 수정이 발생했습니다"),

    // Project
    PROJECT_NOT_FOUND("P001", "프로젝트를 찾을 수 없습니다"),
    PROJECT_CODE_DUPLICATE("P002", "이미 존재하는 프로젝트 코드입니다"),
    INVALID_PROJECT_STATUS_TRANSITION("P003", "유효하지 않은 상태 전이입니다"),
    CONTRACT_REQUIRED_FOR_ACTIVE("P004", "ACTIVE 상태로 전환하려면 계약이 필요합니다"),
    INVALID_ORGANIZATION_ASSIGNMENT("P005", "유효하지 않은 조직 배정입니다"),

    // Contract
    CONTRACT_NOT_FOUND("CT001", "계약을 찾을 수 없습니다"),
    CONTRACT_NUMBER_DUPLICATE("CT002", "이미 존재하는 계약 번호입니다"),
    INVALID_CONTRACT_AMOUNT("CT003", "유효하지 않은 계약 금액입니다"),
    SUB_CONTRACT_EXCEEDS_MASTER("CT004", "하위 계약 금액 합이 상위 계약을 초과합니다"),

    // ProfitLoss
    PROFITLOSS_NOT_FOUND("PL001", "손익 정보를 찾을 수 없습니다"),
    PROFITLOSS_ALREADY_CONFIRMED("PL002", "이미 확정된 손익입니다"),

    // Organization
    ORGANIZATION_NOT_FOUND("O001", "조직을 찾을 수 없습니다"),
    ORGANIZATION_CODE_DUPLICATE("O002", "이미 존재하는 조직 코드입니다"),

    // User
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다"),
    USER_EMAIL_DUPLICATE("U002", "이미 존재하는 이메일입니다"),
    INVALID_CREDENTIALS("U003", "잘못된 인증 정보입니다"),
    USER_LOCKED("U004", "잠긴 계정입니다"),

    // Auth
    UNAUTHORIZED("A001", "인증이 필요합니다"),
    INVALID_TOKEN("A001", "유효하지 않은 토큰입니다"),
    TOKEN_EXPIRED("A002", "토큰이 만료되었습니다"),
    OAUTH_TOKEN_FAILED("A003", "OAuth 토큰 발급에 실패했습니다"),
    OAUTH_USER_INFO_FAILED("A004", "OAuth 사용자 정보 조회에 실패했습니다"),

    // Saju Chart
    CHART_NOT_FOUND("S001", "사주 차트를 찾을 수 없습니다"),
    CHART_ACCESS_DENIED("S002", "사주 차트에 접근 권한이 없습니다"),
    BIRTH_DATE_OUT_OF_RANGE("S003", "지원하지 않는 생년월일 범위입니다"),
    MINOR_BLOCKED("S004", "만 13세 미만은 이용할 수 없습니다"),
    HOUR_AMBIGUOUS("S005", "자시/야자시 구분이 필요합니다. jasiPolicy를 지정해 주세요"),
    TODAY_NOT_ALLOWED_FOR_OTHER("S006", "오늘의 운세는 본인 차트에서만 조회할 수 있습니다"),

    // Share
    SHARE_NOT_FOUND("SH001", "공유 링크를 찾을 수 없거나 만료되었습니다");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
