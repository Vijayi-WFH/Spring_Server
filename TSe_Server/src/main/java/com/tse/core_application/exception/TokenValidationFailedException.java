package com.tse.core_application.exception;

import java.time.LocalDateTime;

public class TokenValidationFailedException extends RuntimeException {

    public TokenValidationFailedException(LocalDateTime expiredTokenDateUTC, String currentTimestampUTC) {
        super("Expired Access Token: Session has expired on " + expiredTokenDateUTC + "." + " The current time is " + currentTimestampUTC);
    }

}
