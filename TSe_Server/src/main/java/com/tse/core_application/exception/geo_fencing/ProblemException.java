package com.tse.core_application.exception.geo_fencing;

import org.springframework.http.HttpStatus;

public class ProblemException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;

    public ProblemException(HttpStatus status, String code, String title, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return getMessage();
    }
}
