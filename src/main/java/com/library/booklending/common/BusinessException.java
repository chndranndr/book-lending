package com.library.booklending.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final BusinessErrorCode code;

    public BusinessException(HttpStatus status, String title, String detail, BusinessErrorCode code) {
        super(detail);
        this.status = status;
        this.title = title;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public BusinessErrorCode getCode() {
        return code;
    }
}
