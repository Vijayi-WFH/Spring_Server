package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AllScheduledTaskIdsRequest {

    Long taskId;
    int isCurrentActivityIndicatorOn;
    boolean userConsent;
    LocalDateTime actualStartDate;

}
