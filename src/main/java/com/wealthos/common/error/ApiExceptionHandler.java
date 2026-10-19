package com.wealthos.common.error;

import com.wealthos.i18n.Messages;
import com.wealthos.system.debug.RequestTraceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final Messages messages;

    public ApiExceptionHandler(Messages messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> business(BusinessException ex) {
        log.info("Rejected: {} {}", ex.getCode(), List.of(ex.getArgs()));
        return build(ex.getStatus(), ex.getCode(), messages.get(ex.getCode(), ex.getArgs()), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        // the validator already translated the messages using the request locale
        List<ApiError.FieldProblem> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiError.FieldProblem(e.getField(), e.getDefaultMessage()))
                .toList();
        log.info("Validation failed: {}", fields);
        return build(HttpStatus.BAD_REQUEST, "error.validation", messages.get("error.validation"), fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> unreadable(Exception ex) {
        log.info("Bad request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "error.badRequest", messages.get("error.badRequest"), List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> notFound(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "error.notFound", messages.get("error.notFound"), List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "error.unexpected", messages.get("error.unexpected"), List.of());
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                                  List<ApiError.FieldProblem> fields) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), code, message, MDC.get(RequestTraceFilter.MDC_KEY), fields));
    }
}
