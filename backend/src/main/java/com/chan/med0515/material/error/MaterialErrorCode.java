package com.chan.med0515.material.error;

import com.chan.med0515.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MaterialErrorCode implements ErrorCode {

    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 원자재입니다."),
    DUPLICATE_PART_CODE(HttpStatus.CONFLICT, "이미 등록된 품번입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
