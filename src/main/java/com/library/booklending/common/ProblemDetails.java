package com.library.booklending.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail create(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("code", code);
        return problemDetail;
    }

    public static ProblemDetail create(HttpStatus status, String title, String detail, BusinessErrorCode code) {
        return create(status, title, detail, code.name());
    }
}
