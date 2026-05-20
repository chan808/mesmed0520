package com.chan.med0515.inspection.error;

import com.chan.med0515.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InspectionErrorCode implements ErrorCode {

    STANDARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 검사 기준서입니다"),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 검사항목입니다"),
    REVISION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 개정 이력입니다");

    private final HttpStatus httpStatus;
    private final String message;
}