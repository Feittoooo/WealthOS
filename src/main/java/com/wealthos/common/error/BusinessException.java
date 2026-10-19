package com.wealthos.common.error;

import org.springframework.http.HttpStatus;

/**
 * A rule the user broke (selling more than they hold, a date in the future...).
 * Carries a message key; ApiExceptionHandler turns it into a translated error response.
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final transient Object[] args;
    private final HttpStatus status;

    public BusinessException(String code, Object... args) {
        this(HttpStatus.UNPROCESSABLE_ENTITY, code, args);
    }

    protected BusinessException(HttpStatus status, String code, Object... args) {
        super(code);
        this.status = status;
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
