package com.tse.core_application.handlers;

import com.tse.core_application.custom.model.RestResponseWithData;
import com.tse.core_application.model.HttpCustomStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CustomResponseHandler {

    public static ResponseEntity<Object> generateCustomResponse(HttpStatus status, String message, Object responseObj) {

        RestResponseWithData restResponseWithData = new RestResponseWithData();
        restResponseWithData.setStatus(status.value());
        restResponseWithData.setMessage(message);
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithData.setTimestamp(UtcDateTime);
        restResponseWithData.setData(responseObj);
        return new ResponseEntity<Object>(restResponseWithData, status);
    }

    public static ResponseEntity<Object> generateCustomResponseForCustom(HttpCustomStatus status, String message, Object responseObj) {
        RestResponseWithData restResponseWithData = new RestResponseWithData();
        restResponseWithData.setStatus(status.value());
        restResponseWithData.setMessage(message);
        restResponseWithData.setTimestamp(getCurrentUTCTimeStamp());
        restResponseWithData.setData(responseObj);

        return ResponseEntity.status(status.value()).body(restResponseWithData);
    }

    public static String getCurrentUTCTimeStamp() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss:SSS Z");
        String UtcDateTime = now.format(formatter);
        return UtcDateTime;
    }

}
