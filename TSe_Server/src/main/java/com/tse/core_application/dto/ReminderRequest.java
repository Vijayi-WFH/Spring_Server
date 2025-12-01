package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReminderRequest {

    @NotNull(message = ErrorConstant.Reminder.TITLE)
    private String reminderTitle;
    private String description;
    @NotNull(message = ErrorConstant.Reminder.REMINDER_DATE)
    private LocalDate reminderDate;
    @NotNull(message = ErrorConstant.Reminder.REMINDER_TIME)
    private LocalTime reminderTime;
    @NotNull(message = ErrorConstant.Reminder.ACCOUNT_ID)
    private Long accountIdCreator;
    private String reminderStatus;
    private Boolean isEarlyReminderSet;
    private LocalDateTime earlyReminderTime;
}
