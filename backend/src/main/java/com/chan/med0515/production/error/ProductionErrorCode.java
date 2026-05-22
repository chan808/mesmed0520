package com.chan.med0515.production.error;

import com.chan.med0515.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductionErrorCode implements ErrorCode {

    MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 생산 모델입니다"),
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 생산 계획입니다"),
    LOT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 생산 lot입니다"),
    INSPECTION_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 검사항목입니다"),
    LOT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "이미 완료된 lot입니다 (PASS/FAIL)"),
    ITEM_NOT_BELONG_TO_MODEL(HttpStatus.BAD_REQUEST, "해당 lot 모델에 속하지 않는 검사항목입니다"),
    DUPLICATE_PLAN(HttpStatus.CONFLICT, "해당 날짜에 동일 모델 계획이 이미 존재합니다"),
    PLAN_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 목표 수량을 달성한 계획입니다"),
    TARGET_QTY_LESS_THAN_PASS_COUNT(HttpStatus.BAD_REQUEST, "목표 수량은 현재 합격 수량보다 적을 수 없습니다"),
    MEASURED_VALUE_REQUIRED(HttpStatus.BAD_REQUEST, "수치 측정 항목은 measuredValue가 필수입니다"),
    RESULT_REQUIRED(HttpStatus.BAD_REQUEST, "육안 검사 항목은 result(PASS/NG)가 필수입니다");

    private final HttpStatus httpStatus;
    private final String message;
}