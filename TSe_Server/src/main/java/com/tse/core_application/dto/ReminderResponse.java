package com.tse.core_application.dto;

import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReminderResponse {

    private Long reminderId;
    private String reminderTitle;
    private String description;
    private LocalDate reminderDate;
    private LocalTime reminderTime;
    private EmailFirstLastAccountId userDetails;
    private String reminderStatus;
    private Boolean isEarlyReminderSet;
    private LocalDateTime earlyReminderTime;

}
