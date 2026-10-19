package com.wealthos.common.error;

import java.util.List;

/**
 * Body of every error response. message is already translated (Accept-Language);
 * code is the message key, handy for the frontend and for debugging.
 */
public record ApiError(int status, String code, String message, String requestId, List<FieldProblem> fields) {

    public record FieldProblem(String field, String message) {
    }
}
