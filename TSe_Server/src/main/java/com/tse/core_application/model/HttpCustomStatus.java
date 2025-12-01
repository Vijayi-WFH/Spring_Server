package com.tse.core_application.model;

import org.springframework.http.HttpStatus;

public enum HttpCustomStatus {

    INVALID_TOKEN(499, HttpStatus.Series.CLIENT_ERROR, "Invalid Token"),
    DUPLICATE_WORK_ITEM(496, HttpStatus.Series.CLIENT_ERROR, "Duplicate Work Item Present");

    private final int value;
    private final HttpStatus.Series series;
    private final String reasonPhrase;

    HttpCustomStatus(int value, HttpStatus.Series series, String reasonPhrase) {
        this.value = value;
        this.series = series;
        this.reasonPhrase = reasonPhrase;
    }

    public int value() {
        return this.value;
    }

    public String reasonPhrase() {
        return this.reasonPhrase;
    }
}
