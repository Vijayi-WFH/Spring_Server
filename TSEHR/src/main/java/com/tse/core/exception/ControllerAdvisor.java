package com.tse.core.exception;

import com.tse.core.custom.model.RestResponseWithoutData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {

    public static String getCurrentUTCTimeStamp() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss:SSS Z");
        String UtcDateTime = now.format(formatter);
        return UtcDateTime;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(LeaveApplicationValidationException.class)
    public ResponseEntity<Object> handleLeaveApplicationValidationException(LeaveApplicationValidationException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
    }
}
