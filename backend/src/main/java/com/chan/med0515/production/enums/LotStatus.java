package com.chan.med0515.production.enums;

public enum LotStatus {
    IN_PROGRESS,   // 검사 진행 중 (NG 항목 존재 또는 미검사)
    PASS,          // 전 항목 합격
    FAIL           // 수동 불합격 처리
}