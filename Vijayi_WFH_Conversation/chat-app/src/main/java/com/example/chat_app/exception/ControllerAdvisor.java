package com.example.chat_app.exception;

import com.example.chat_app.custom.model.RestResponseWithoutData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@RestControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {

    private static final Logger log = LogManager.getLogger(ControllerAdvisor.class);

    @ExceptionHandler(UnauthorizedLoginException.class)
    public ResponseEntity<Object> handleUnauthorizedException(UnauthorizedLoginException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        log.error("Unauthorized login for accountIds={}, requestURI={}", ThreadContext.get("accountIds"), ThreadContext.get("requestURI"), e);
        ThreadContext.clearMap();
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<Object> handleUnauthorizedActionException(UnauthorizedActionException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        log.error("Unauthorized Action for accountIds={}, requestURI={}", ThreadContext.get("accountIds"), ThreadContext.get("requestURI"), e);
        ThreadContext.clearMap();
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<Object> handleValidationFailedException(ValidationFailedException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        log.error("Validation Failed error for accountIds={}, requestURI={}", ThreadContext.get("accountIds"), ThreadContext.get("requestURI"), e);
        ThreadContext.clearMap();
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Object> handleFileNotFoundException(FileNotFoundException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        log.error("File Not Found error for accountIds={}, requestURI={}", ThreadContext.get("accountIds"), ThreadContext.get("requestURI"), e);
        ThreadContext.clearMap();
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        log.error("IllegalStateExp: error for accountIds={}, requestURI={}", ThreadContext.get("accountIds"), ThreadContext.get("requestURI"), e);
        ThreadContext.clearMap();
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.METHOD_NOT_ALLOWED);
    }

    public static String getCurrentUTCTimeStamp() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss:SSS Z");
        String UtcDateTime = now.format(formatter);
        return UtcDateTime;
    }

    /**
     * Handle all sort of exceptions that are Uncaught/Unhandled.
     * */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception e, WebRequest request) {
        RestResponseWithoutData response = new RestResponseWithoutData();
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMessage(e.getMessage());
        response.setTimestamp(getCurrentUTCTimeStamp());
        log.error("Unexpected error for accountIds={}, requestURI={}", ThreadContext.get("accountIds"), ThreadContext.get("requestURI"), e);
        ThreadContext.clearMap();
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Object> handleNullPointerException(NullPointerException e, WebRequest request) {
        RestResponseWithoutData response = new RestResponseWithoutData();
        response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
        response.setMessage("Unexpected error: " + e.getMessage());
        response.setTimestamp(getCurrentUTCTimeStamp());
        log.error("Unexpected error for accountIds={}, requestURI={}", ThreadContext.get("accountIds"), ThreadContext.get("requestURI"), e);
        ThreadContext.clearMap();
        return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Object> handleRestClientException(RestClientException ex, WebRequest request) {
        RestResponseWithoutData response = new RestResponseWithoutData();
        response.setStatus(HttpStatus.BAD_GATEWAY.value());
        response.setMessage(ex.getMessage());
        response.setTimestamp(getCurrentUTCTimeStamp());
        log.error("RestTemplate connection error for accountIds={}, requestURI={}", ThreadContext.get("accountIds"), ThreadContext.get("requestURI"), ex);
        ThreadContext.clearMap();
        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex, WebRequest request) {
        RestResponseWithoutData response = new RestResponseWithoutData();
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setMessage(ex.getMessage());
        response.setTimestamp(getCurrentUTCTimeStamp());
        log.error("Unexpected error for accountIds={}, requestURI={}", ThreadContext.get("accountIds"), ThreadContext.get("requestURI"), ex);
        ThreadContext.clearMap();
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
