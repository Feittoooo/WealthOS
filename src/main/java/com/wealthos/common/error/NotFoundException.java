package com.wealthos.common.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

    public NotFoundException(String code, Object... args) {
        super(HttpStatus.NOT_FOUND, code, args);
    }
}
