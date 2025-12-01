package com.tse.core_application.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReferenceMeetingException extends RuntimeException{

    public final Boolean isNotificationOnCooldown;

    public ReferenceMeetingException(String errorMessage, Boolean isNotificationOnCooldown){
        super(errorMessage);
        this.isNotificationOnCooldown = isNotificationOnCooldown;
    }
}
