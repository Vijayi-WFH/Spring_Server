package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.exception.ValidationFailedException;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Data
public class RecurrenceScheduleDTO {

    //    @NotNull(message = ErrorConstant.RecurTask.START_DATE)
    private LocalDate startDate; // start date of the range

    //    @NotNull(message = ErrorConstant.RecurTask.END_DATE)
    private LocalDate endDate; // end date of the range

    //    @NotNull(message = ErrorConstant.RecurTask.SELECTED_DATE)
    private LocalDate selectedDate;

    @NotNull(message = ErrorConstant.RecurTask.RECUR_TYPE)
    private RecurrenceType recurrenceType;

    @Max(value = 100, message = ErrorConstant.RecurTask.OCCURENCE_LIMIT)
    private Integer numberOfOccurrences; // Number of times the event should occur -- either startDate & endDate should be provided or numberOfOccurrences

    private List<String> recurDays; // values : ["MONDAY", "TUESDAY"] in case of weekly and ["1", "2"] in case of monthly

    @NotNull(message = ErrorConstant.RecurTask.OCCURRENCE_DURATION)
    private Integer occurrenceDuration; // number of days

    private CustomRecurrenceOption customRecurrenceOption; // For quarterly and half-yearly custom options

    public enum RecurrenceType {
        DAILY, WEEKLY, EVERY_TWO_WEEKS, MONTHLY, YEARLY, DAILY_BUSINESS_DAY, QUARTERLY, HALF_YEARLY, CUSTOM
    }

    @Getter
    @Setter
    public static class CustomRecurrenceOption {
        @NotNull(message = ErrorConstant.RecurTask.CUSTOM_RECURRENCE_TYPE)
        private RecurrenceType customRecurrenceType; // allowed values: QUARTERLY, HALF_YEARLY

        @NotNull(message = ErrorConstant.RecurTask.ORDINAL)
        private Ordinal ordinal;

        @NotNull(message = ErrorConstant.RecurTask.DAY_TYPE)
        private DayType dayType;
    }

    public enum Ordinal {
        FIRST, SECOND, SECOND_LAST, LAST
    }

    public enum DayType {
        DAY, WORKING_DAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    public static void validateWeeklyRecurDays(List<String> recurDays) {
        for (String day : recurDays) {
            try {
                DayOfWeek.valueOf(day.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationFailedException("Invalid day of week: " + day);
            }
        }
    }

    public static void validateMonthlyRecurDays(List<String> recurDays) {
        for (String day : recurDays) {
            try {
                int dayOfMonth = Integer.parseInt(day);
                if (dayOfMonth < 1 || dayOfMonth > 31) {
                    throw new ValidationFailedException("Day of month must be between 1 and 31");
                }
            } catch (NumberFormatException e) {
                throw new ValidationFailedException("Invalid day of month: " + day);
            }
        }
    }

    public static void validateYearlyRecurDays(List<String> recurDays) {
        // Define the formatter for "MM-dd" format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (String day : recurDays) {
            try {
                // Parse the string to check if it's in the valid "MM-dd" format
                MonthDay.parse("--" + day, DateTimeFormatter.ofPattern("--MM-dd"));
            } catch (DateTimeParseException e) {
                throw new ValidationFailedException("Invalid date format for yearly recurrence: " + day + ". Required format: MM-dd");
            }
        }
    }

}
