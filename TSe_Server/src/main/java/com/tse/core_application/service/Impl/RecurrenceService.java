package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.HolidayOffDayInfo;
import com.tse.core_application.dto.RecurrenceScheduleDTO;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.EntityPreference;
import com.tse.core_application.model.HolidayOffDay;
import com.tse.core_application.model.UserAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecurrenceService {

    private static final Logger logger = LogManager.getLogger(RecurrenceService.class.getName());
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private EntityPreferenceService entityPreferenceService;

    /**
     * generates the custom recurring dates based on the schedule filters
     */
    public List<LocalDate[]> generateRecurringDates(RecurrenceScheduleDTO scheduleDTO, Long userAccountId) {
        switch (scheduleDTO.getRecurrenceType()) {
            case DAILY:
                return generateDailyRecurringDates(scheduleDTO);
            case WEEKLY:
                return generateWeeklyRecurringDates(scheduleDTO, 1);
            case EVERY_TWO_WEEKS:
                return generateWeeklyRecurringDates(scheduleDTO, 2);
            case MONTHLY:
                return generateMonthlyRecurringDates(scheduleDTO);
            case YEARLY:
                return generateYearlyRecurringDates(scheduleDTO);
            case DAILY_BUSINESS_DAY:
                return generateBusinessDayRecurringDates(scheduleDTO, userAccountId);
            case QUARTERLY:
            case HALF_YEARLY:
                return generateQuarterlyOrHalfYearlyRecurringDates(scheduleDTO);
            case CUSTOM:
                if (scheduleDTO.getCustomRecurrenceOption().getCustomRecurrenceType().equals(RecurrenceScheduleDTO.RecurrenceType.QUARTERLY)) {
                    return generateCustomRecurringDatesForQuarter(scheduleDTO, userAccountId);
                } else if (scheduleDTO.getCustomRecurrenceOption().getCustomRecurrenceType().equals(RecurrenceScheduleDTO.RecurrenceType.HALF_YEARLY)) {
                    return generateCustomHalfYearlyRecurringDates(scheduleDTO, userAccountId);
                }
            default:
                throw new IllegalArgumentException("Invalid recurrence type");
        }
    }


    /**
     * generate expected dates for daily recurrence
     */
    private List<LocalDate[]> generateDailyRecurringDates(RecurrenceScheduleDTO scheduleDTO) {
        List<LocalDate[]> dates = new ArrayList<>();
        LocalDate current = scheduleDTO.getStartDate();

        if (scheduleDTO.getEndDate() != null) {
            // Loop from startDate to endDate
            while (!current.isAfter(scheduleDTO.getEndDate())) {
                dates.add(new LocalDate[]{current, current.plusDays(scheduleDTO.getOccurrenceDuration())});
                current = current.plusDays(1);
            }
        } else {
            // Loop based on numberOfOccurrences
            int occurrences = scheduleDTO.getNumberOfOccurrences() != null ? scheduleDTO.getNumberOfOccurrences() : 0;
            for (int i = 0; i < occurrences; i++) {
                dates.add(new LocalDate[]{current, current.plusDays(scheduleDTO.getOccurrenceDuration())});
                current = current.plusDays(1);
            }
        }

        return dates;
    }


    /**
     * generate expected dates for weekly/ bi-weekly recurrence
     */
    private List<LocalDate[]> generateWeeklyRecurringDates(RecurrenceScheduleDTO scheduleDTO, int weeksBetweenRecurrences) {
        List<LocalDate[]> dates = new ArrayList<>();
        LocalDate current = scheduleDTO.getStartDate();
        List<DayOfWeek> recurDays = scheduleDTO.getRecurDays().stream()
                .map(String::toUpperCase)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toList());

        if (scheduleDTO.getEndDate() != null) {
            while (!current.isAfter(scheduleDTO.getEndDate())) {
                LocalDate weekStart = current;
                for (DayOfWeek day : recurDays) {
                    LocalDate nextOccurrence = weekStart.with(TemporalAdjusters.nextOrSame(day));
                    if (!nextOccurrence.isAfter(scheduleDTO.getEndDate()) && !nextOccurrence.isBefore(weekStart)) {
                        dates.add(new LocalDate[]{nextOccurrence, nextOccurrence.plusDays(scheduleDTO.getOccurrenceDuration())});
                    }
                }
                // Increment current for the next iteration
                current = current.plusWeeks(weeksBetweenRecurrences);
            }
        } else if (scheduleDTO.getNumberOfOccurrences() != null) {
            int occurrences = 0;
            while (occurrences < scheduleDTO.getNumberOfOccurrences() * recurDays.size()) {
                LocalDate weekStart = current;
                for (DayOfWeek day : recurDays) {
                    LocalDate nextOccurrence = weekStart.with(TemporalAdjusters.nextOrSame(day));
                    if (nextOccurrence.isEqual(weekStart) || nextOccurrence.isAfter(weekStart)) {
                        dates.add(new LocalDate[]{nextOccurrence, nextOccurrence.plusDays(scheduleDTO.getOccurrenceDuration())});
                        occurrences++;
                    }
                    if (occurrences >= scheduleDTO.getNumberOfOccurrences() * recurDays.size()) {
                        break;
                    }
                }
                // Increment current for the next iteration
                current = current.plusWeeks(weeksBetweenRecurrences);
            }
        }

        return dates;
    }


    /**
     * generate expected dates for monthly recurrence. If a month doesn't have a specific date, it'll be skipped
     * example - selectedDate = 30 Jan, so recurrence dates will be 30 Jan, 30 Mar, 30 Apr ...
     * User can select a number of recur days from the values (1..31)
     */
    private List<LocalDate[]> generateMonthlyRecurringDates(RecurrenceScheduleDTO scheduleDTO) {
        List<LocalDate[]> dates = new ArrayList<>();
        List<Integer> recurDays = scheduleDTO.getRecurDays().stream()
                .map(Integer::parseInt)
                .sorted()
                .collect(Collectors.toList());

        if (scheduleDTO.getNumberOfOccurrences() != null) {
            for (Integer day : recurDays) {
                LocalDate current = scheduleDTO.getStartDate().withDayOfMonth(1); // Start from the first of the start month
                int occurrences = 0;
                while (occurrences < scheduleDTO.getNumberOfOccurrences()) {
                    if (day <= current.lengthOfMonth()) { // Check if the day exists in the month
                        LocalDate nextOccurrence = current.withDayOfMonth(day);
                        if (!nextOccurrence.isBefore(scheduleDTO.getStartDate())) { // Check if the occurrence is after the start date
                            dates.add(new LocalDate[]{nextOccurrence, nextOccurrence.plusDays(scheduleDTO.getOccurrenceDuration())});
                            occurrences++;
                        }
                    }
                    current = current.plusMonths(1).withDayOfMonth(1); // Move to the first of the next month
                }
            }
        } else if (scheduleDTO.getEndDate() != null) {
            LocalDate current = scheduleDTO.getStartDate();
            while (!current.isAfter(scheduleDTO.getEndDate())) {
                for (Integer day : recurDays) {
                    if (day <= current.lengthOfMonth() && !current.withDayOfMonth(day).isBefore(scheduleDTO.getStartDate())) {
                        LocalDate nextOccurrence = current.withDayOfMonth(day);
                        if (!nextOccurrence.isAfter(scheduleDTO.getEndDate())) {
                            dates.add(new LocalDate[]{nextOccurrence, nextOccurrence.plusDays(scheduleDTO.getOccurrenceDuration())});
                        }
                    }
                }
                current = current.plusMonths(1).withDayOfMonth(1);
            }
        }

        return dates;
    }


    /**
     * generate expected dates for yearly recurrence. If a year doesn't have a specific date, it'll be skipped
     * User can give list of days in year on which the task should be repeated
     * example - selectedDate = 29 Feb, so recurrence dates will be 29 Feb after every 4 years
     */
//    private List<LocalDate[]> generateYearlyRecurringDates(RecurrenceScheduleDTO scheduleDTO) {
//        List<LocalDate[]> dates = new ArrayList<>();
//        LocalDate current = scheduleDTO.getStartDate();
//        int startYear = current.getYear();
//
//        // Convert MM-dd strings to LocalDate, using the start year as the base year.
//        List<LocalDate> recurDates = scheduleDTO.getRecurDays().stream()
//                .map(dateStr -> LocalDate.parse(dateStr + "-" + startYear, DateTimeFormatter.ofPattern("MM-dd-yyyy")))
//                .collect(Collectors.toList());
//
//        if (scheduleDTO.getEndDate() != null) {
//            while (!current.isAfter(scheduleDTO.getEndDate())) {
//                for (LocalDate recurDate : recurDates) {
//                    LocalDate nextOccurrence = LocalDate.of(current.getYear(), recurDate.getMonth(), recurDate.getDayOfMonth());
//                    if (!Year.isLeap(current.getYear()) && nextOccurrence.getMonth() == Month.FEBRUARY && nextOccurrence.getDayOfMonth() == 29) {
//                        nextOccurrence = nextOccurrence.withDayOfMonth(28);
//                    }
//                    if (!nextOccurrence.isBefore(current) && !nextOccurrence.isAfter(scheduleDTO.getEndDate())) {
//                        dates.add(new LocalDate[]{nextOccurrence, nextOccurrence.plusDays(scheduleDTO.getOccurrenceDuration())});
//                    }
//                }
//                current = current.plusYears(1);
//            }
//        } else if (scheduleDTO.getNumberOfOccurrences() != null) {
//            int occurrences = 0;
//            while (occurrences < scheduleDTO.getNumberOfOccurrences()) {
//                for (LocalDate recurDate : recurDates) {
//                    LocalDate nextOccurrence = LocalDate.of(current.getYear(), recurDate.getMonth(), recurDate.getDayOfMonth());
//                    if (!Year.isLeap(current.getYear()) && nextOccurrence.getMonth() == Month.FEBRUARY && nextOccurrence.getDayOfMonth() == 29) {
//                        nextOccurrence = nextOccurrence.withDayOfMonth(28);
//                    }
//                    if (nextOccurrence.isEqual(current) || nextOccurrence.isAfter(current)) {
//                        dates.add(new LocalDate[]{nextOccurrence, nextOccurrence.plusDays(scheduleDTO.getOccurrenceDuration())});
//                        occurrences++;
//                    }
//                    if (occurrences >= scheduleDTO.getNumberOfOccurrences()) {
//                        break;
//                    }
//                }
//                // Move to the start of the next year after checking all dates
//                current = LocalDate.of(current.getYear() + 1, 1, 1);
//            }
//        }
//
//        return dates;
//    }

    private List<LocalDate[]> generateYearlyRecurringDates(RecurrenceScheduleDTO scheduleDTO) {
        List<LocalDate[]> dates = new ArrayList<>();
        int startYear = scheduleDTO.getStartDate().getYear();

        // Convert MM-dd strings to LocalDate, using the start year as the base year.
        List<LocalDate> recurDates = scheduleDTO.getRecurDays().stream()
                .map(dateStr -> LocalDate.parse(dateStr + "-" + startYear, DateTimeFormatter.ofPattern("MM-dd-yyyy")))
                .collect(Collectors.toList());

        if (scheduleDTO.getNumberOfOccurrences() != null) {
            for (LocalDate recurDate : recurDates) {
                LocalDate current = LocalDate.of(startYear, recurDate.getMonth(), recurDate.getDayOfMonth());
                int occurrences = 0;
                while (occurrences < scheduleDTO.getNumberOfOccurrences()) {
                    if (!Year.isLeap(current.getYear()) && current.getMonth() == Month.FEBRUARY && current.getDayOfMonth() == 29) {
                        current = current.withDayOfMonth(28);
                    }
                    if (!current.isBefore(scheduleDTO.getStartDate())) {
                        dates.add(new LocalDate[]{current, current.plusDays(scheduleDTO.getOccurrenceDuration())});
                        occurrences++;
                    }
                    current = current.plusYears(1);
                }
            }
        } else if (scheduleDTO.getEndDate() != null) {
            for (LocalDate recurDate : recurDates) {
                LocalDate current = LocalDate.of(startYear, recurDate.getMonth(), recurDate.getDayOfMonth());
                while (!current.isAfter(scheduleDTO.getEndDate())) {
                    if (!Year.isLeap(current.getYear()) && current.getMonth() == Month.FEBRUARY && current.getDayOfMonth() == 29) {
                        current = current.withDayOfMonth(28);
                    }
                    if (!current.isBefore(scheduleDTO.getStartDate()) && !current.isAfter(scheduleDTO.getEndDate())) {
                        dates.add(new LocalDate[]{current, current.plusDays(scheduleDTO.getOccurrenceDuration())});
                    }
                    current = current.plusYears(1);
                }
            }
        }

        return dates;
    }


    /**
     * generate expected dates for quarterly or half-yearly recurrence. Either date range startDate and endDate are provided
     * or startDate and numberOfOccurrences. Only a single date is selectable example if Feb 10 is selected, the generated
     * dates will be Feb 10, May 10, Aug 10...
     * If the given date is not present in the upcoming month that will be adjusted accordingly
     */
    private List<LocalDate[]> generateQuarterlyOrHalfYearlyRecurringDates(RecurrenceScheduleDTO scheduleDTO) {
        List<LocalDate[]> dates = new ArrayList<>();
        LocalDate current = scheduleDTO.getSelectedDate();
        int monthsToAdd = scheduleDTO.getRecurrenceType() == RecurrenceScheduleDTO.RecurrenceType.QUARTERLY ? 3 : 6;
        int targetDay = current.getDayOfMonth();

        // Add the selectedDate if it's within the range before starting the loop
        if ((scheduleDTO.getEndDate() == null || !current.isAfter(scheduleDTO.getEndDate())) &&
                (scheduleDTO.getNumberOfOccurrences() == null || dates.size() < scheduleDTO.getNumberOfOccurrences())) {
            dates.add(new LocalDate[]{current, current.plusDays(scheduleDTO.getOccurrenceDuration())});
        }

        // Adjust the current date to the next occurrence month
        current = current.plusMonths(monthsToAdd).withDayOfMonth(1);  // Move to the 1st of the next month to avoid end-of-month issues

        while ((scheduleDTO.getEndDate() == null || !current.isAfter(scheduleDTO.getEndDate())) &&
                (scheduleDTO.getNumberOfOccurrences() == null || dates.size() < scheduleDTO.getNumberOfOccurrences())) {

            int daysInMonth = current.lengthOfMonth();
            int dayToUse = Math.min(targetDay, daysInMonth);
            LocalDate nextOccurrence = current.withDayOfMonth(dayToUse);

            if ((scheduleDTO.getEndDate() == null || !nextOccurrence.isAfter(scheduleDTO.getEndDate()))) {
                dates.add(new LocalDate[]{nextOccurrence, nextOccurrence.plusDays(scheduleDTO.getOccurrenceDuration())});
            }

            // Prepare for the next cycle
            current = current.plusMonths(monthsToAdd).withDayOfMonth(1); // Adjust to the first of the month
        }

        return dates;
    }

    //    private List<LocalDate[]> generateQuarterlyOrHalfYearlyRecurringDates(RecurrenceScheduleDTO scheduleDTO) {
//        List<LocalDate[]> dates = new ArrayList<>();
//        LocalDate current = scheduleDTO.getStartDate();
//        int monthsToAdd = scheduleDTO.getRecurrenceType() == RecurrenceScheduleDTO.RecurrenceType.QUARTERLY ? 3 : 6;
//        int targetDay = scheduleDTO.getSelectedDate().getDayOfMonth();
//
//        while (scheduleDTO.getEndDate() == null || !current.isAfter(scheduleDTO.getEndDate())) {
//            // Adjust for the target day or the last day of the month if the target day exceeds the month length
//            int adjustedDay = Math.min(current.plusMonths(monthsToAdd).lengthOfMonth(), targetDay);
//            LocalDate nextOccurrence = current.plusMonths(monthsToAdd).withDayOfMonth(adjustedDay);
//
//            // Check if nextOccurrence falls within the range
//            if (!nextOccurrence.isBefore(scheduleDTO.getStartDate()) && (scheduleDTO.getEndDate() == null || !nextOccurrence.isAfter(scheduleDTO.getEndDate()))) {
//                dates.add(new LocalDate[]{nextOccurrence, nextOccurrence.plusDays(scheduleDTO.getOccurrenceDuration())});
//            }
//
//            // Break the loop if the number of occurrences is reached (for the case without an end date)
//            if (scheduleDTO.getNumberOfOccurrences() != null && dates.size() >= scheduleDTO.getNumberOfOccurrences()) {
//                break;
//            }
//
//            // Prepare for the next cycle
//            current = nextOccurrence;
//        }
//
//        return dates;
//    }

    /**
     * generate expected dates for daily recurrence but only on the business days (doesn't include off days in a week or holidays in an org.
     * If a year doesn't have a specific date, it'll be skipped
     */
    private List<LocalDate[]> generateBusinessDayRecurringDates(RecurrenceScheduleDTO scheduleDTO, Long userAccountId) {
        List<LocalDate[]> dates = new ArrayList<>();
        LocalDate current = scheduleDTO.getStartDate();
        HolidayOffDayInfo holidayOffDayInfo = getHolidaysAndOffDaysBasedOnEntityPreference(userAccountId);

        if (scheduleDTO.getEndDate() != null) {
            // Loop from startDate to endDate, considering only business days
            while (!current.isAfter(scheduleDTO.getEndDate())) {
                if (isBusinessDay(current, holidayOffDayInfo)) {
                    dates.add(new LocalDate[]{current, current.plusDays(scheduleDTO.getOccurrenceDuration())});
                }
                current = current.plusDays(1);
            }
        } else if (scheduleDTO.getNumberOfOccurrences() != null) {
            int occurrencesAdded = 0;
            while (occurrencesAdded < scheduleDTO.getNumberOfOccurrences()) {
                if (isBusinessDay(current, holidayOffDayInfo)) {
                    dates.add(new LocalDate[]{current, current.plusDays(scheduleDTO.getOccurrenceDuration())});
                    occurrencesAdded++;
                }
                current = current.plusDays(1);
            }
        }

        return dates;
    }


    /**
     * generate expected dates for custom logic of quarterly occurrence
     */
//    private List<LocalDate[]> generateCustomRecurringDatesForQuarter(RecurrenceScheduleDTO scheduleDTO, Long userAccountId) {
//        List<LocalDate[]> dates = new ArrayList<>();
//        LocalDate current = scheduleDTO.getSelectedDate();
//        int year = current.getYear();
//        int quarter = getQuarter(current);
//
//        while (!current.isAfter(scheduleDTO.getEndDate())) {
//            LocalDate targetDate = null;
//
//            if (scheduleDTO.getCustomRecurrenceOption().getDayType() == RecurrenceScheduleDTO.DayType.DAY) {
//                targetDate = findPositionalDayOfTheQuarter(year, quarter, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
//                // where are we incrementing the year and quarter?
//            } else if (scheduleDTO.getCustomRecurrenceOption().getDayType() == RecurrenceScheduleDTO.DayType.WORKING_DAY) {
//                targetDate = findPositionalBusinessDayOfTheQuarter(year, quarter, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name(), userAccountId);
//            } else {
//                DayOfWeek dayOfWeek = convertDayTypeToDayOfWeek(scheduleDTO.getCustomRecurrenceOption().getDayType());
//                if (scheduleDTO.getCustomRecurrenceOption().getOrdinal() == RecurrenceScheduleDTO.Ordinal.FIRST || scheduleDTO.getCustomRecurrenceOption().getOrdinal() == RecurrenceScheduleDTO.Ordinal.SECOND) {
//                    targetDate = findSpecificDayOfTheQuarter(year, quarter, dayOfWeek, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
//                } else {
//                    targetDate = findLastSpecificDayOfTheQuarter(year, quarter, dayOfWeek, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
//                }
//            }
//
//            if (targetDate != null && !targetDate.isAfter(scheduleDTO.getEndDate()) && !targetDate.isBefore(current)) {
//                dates.add(new LocalDate[]{targetDate, targetDate.plusDays(scheduleDTO.getOccurrenceDuration())});
//            }
//
//            // next iteration logic.
//            if (quarter == 4) {
//                quarter = 1;
//                year++;
//            } else {
//                quarter++;
//            }
//
//            // Update current to the start of the next quarter to continue the loop.
//            current = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
//        }
//
//        return dates;
//    }

    private List<LocalDate[]> generateCustomRecurringDatesForQuarter(RecurrenceScheduleDTO scheduleDTO, Long userAccountId) {
        List<LocalDate[]> dates = new ArrayList<>();
        LocalDate current = scheduleDTO.getStartDate();
        int year = current.getYear();
        int quarter = getQuarter(current);

        int occurrences = 0;
        int maxOccurrences = scheduleDTO.getNumberOfOccurrences() != null ? scheduleDTO.getNumberOfOccurrences() : Integer.MAX_VALUE;

        while ((scheduleDTO.getEndDate() == null || !current.isAfter(scheduleDTO.getEndDate())) && occurrences < maxOccurrences) {
            LocalDate targetDate = null;

            if (scheduleDTO.getCustomRecurrenceOption().getDayType() == RecurrenceScheduleDTO.DayType.DAY) {
                targetDate = findPositionalDayOfTheQuarter(year, quarter, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
            } else if (scheduleDTO.getCustomRecurrenceOption().getDayType() == RecurrenceScheduleDTO.DayType.WORKING_DAY) {
                targetDate = findPositionalBusinessDayOfTheQuarter(year, quarter, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name(), userAccountId);
            } else {
                DayOfWeek dayOfWeek = convertDayTypeToDayOfWeek(scheduleDTO.getCustomRecurrenceOption().getDayType());
                if (scheduleDTO.getCustomRecurrenceOption().getOrdinal() == RecurrenceScheduleDTO.Ordinal.FIRST || scheduleDTO.getCustomRecurrenceOption().getOrdinal() == RecurrenceScheduleDTO.Ordinal.SECOND) {
                    targetDate = findSpecificDayOfTheQuarter(year, quarter, dayOfWeek, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
                } else {
                    targetDate = findLastSpecificDayOfTheQuarter(year, quarter, dayOfWeek, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
                }
            }

            if (targetDate != null && (scheduleDTO.getEndDate() == null || !targetDate.isAfter(scheduleDTO.getEndDate())) && !targetDate.isBefore(current)) {
                dates.add(new LocalDate[]{targetDate, targetDate.plusDays(scheduleDTO.getOccurrenceDuration())});
                occurrences++;
            }

            // Increment year and quarter for the next iteration
            if (quarter == 4) {
                quarter = 1;
                year++;
            } else {
                quarter++;
            }
            current = LocalDate.of(year, quarter * 3 - 2, 1); // Set to the first month of the next quarter
        }

        return dates;
    }


    /**
     * find first/ second specific positional day in the quarter example first Monday of the quarter
     */
    private LocalDate findSpecificDayOfTheQuarter(int year, int quarter, DayOfWeek day, String position) {
        LocalDate startOfQuarter = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate targetDate = startOfQuarter.with(TemporalAdjusters.nextOrSame(day));

        if ("Second".equals(position)) {
            // Add 7 days to get the second occurrence
            targetDate = targetDate.plusWeeks(1);
        }

        return targetDate;
    }


    /**
     * find last/ second last specific positional day in the quarter example last monday of quarter
     */
    private LocalDate findLastSpecificDayOfTheQuarter(int year, int quarter, DayOfWeek day, String position) {
        LocalDate endOfQuarter = LocalDate.of(year, quarter * 3, 1).with(TemporalAdjusters.lastDayOfMonth());
        LocalDate targetDate = endOfQuarter.with(TemporalAdjusters.previousOrSame(day));

        if ("Second Last".equals(position)) {
            // Subtract 7 days to get the second last occurrence
            targetDate = targetDate.minusWeeks(1);
        }

        return targetDate;
    }


    /**
     * returns a particular day in the quarter example first or last day of quarter
     */
    private LocalDate findPositionalDayOfTheQuarter(int year, int quarter, String position) {
        LocalDate startOfQuarter = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate endOfQuarter = startOfQuarter.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

        switch (position) {
            case "FIRST":
                return startOfQuarter;
            case "SECOND":
                return startOfQuarter.plusDays(1);
            case "SECOND_LAST":
                return endOfQuarter.minusDays(1);
            case "LAST":
                return endOfQuarter;
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
    }


    /**
     * find the positional business day in the quarter
     */
    private LocalDate findPositionalBusinessDayOfTheQuarter(int year, int quarter, String position, Long userAccountId) {
        LocalDate date = findPositionalDayOfTheQuarter(year, quarter, position);
        HolidayOffDayInfo holidayOffDayInfo = getHolidaysAndOffDaysBasedOnEntityPreference(userAccountId);

        switch (position) {
            case "First":
            case "Second":
                while (!isBusinessDay(date, holidayOffDayInfo)) {
                    date = date.plusDays(1);
                }
                if ("Second".equals(position)) {
                    LocalDate nextDay = date.plusDays(1);
                    while (!isBusinessDay(nextDay, holidayOffDayInfo)) {
                        nextDay = nextDay.plusDays(1);
                    }
                    return nextDay;
                }
                return date;
            case "Second Last":
            case "Last":
                // Find the last business day first.
                while (!isBusinessDay(date, holidayOffDayInfo)) {
                    date = date.minusDays(1);  // Move backward to find the last business day.
                }
                if ("Second Last".equals(position)) {
                    LocalDate previousDay = date.minusDays(1);
                    while (!isBusinessDay(previousDay, holidayOffDayInfo)) {
                        previousDay = previousDay.minusDays(1);
                    }
                    return previousDay;
                }
                return date;  // Return the last business day if the position is "Last".

            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
    }


    /**
     * generate expected dates for custom logic of half-yearly occurrence
     */
//    private List<LocalDate[]> generateCustomHalfYearlyRecurringDates(RecurrenceScheduleDTO scheduleDTO, Long userAccountId) {
//        List<LocalDate[]> dates = new ArrayList<>();
//        LocalDate current = scheduleDTO.getSelectedDate();
//        int year = current.getYear();
//        int halfYear = current.getMonthValue() <= 6 ? 1 : 2; // Determine if the date is in the first or second half of the year.
//
//        while (!current.isAfter(scheduleDTO.getEndDate())) {
//            LocalDate targetDate;
//
//            // Determine the target date based on the custom recurrence option.
//            if (scheduleDTO.getCustomRecurrenceOption().getDayType() == RecurrenceScheduleDTO.DayType.DAY) {
//                targetDate = findPositionalDayOfHalfYear(year, halfYear, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
//            } else if (scheduleDTO.getCustomRecurrenceOption().getDayType() == RecurrenceScheduleDTO.DayType.WORKING_DAY) {
//                targetDate = findPositionalBusinessDayOfHalfYear(year, halfYear, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name(), userAccountId);
//            } else {
//                DayOfWeek dayOfWeek = convertDayTypeToDayOfWeek(scheduleDTO.getCustomRecurrenceOption().getDayType());
//                if (scheduleDTO.getCustomRecurrenceOption().getOrdinal() == RecurrenceScheduleDTO.Ordinal.FIRST || scheduleDTO.getCustomRecurrenceOption().getOrdinal() == RecurrenceScheduleDTO.Ordinal.SECOND) {
//                    targetDate = findSpecificDayOfHalfYear(year, halfYear, dayOfWeek, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
//                } else {
//                    targetDate = findLastSpecificDayOfHalfYear(year, halfYear, dayOfWeek, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
//                }
//            }
//
//            if (!targetDate.isAfter(scheduleDTO.getEndDate()) && !targetDate.isBefore(current)) {
//                dates.add(new LocalDate[]{targetDate, targetDate.plusDays(scheduleDTO.getOccurrenceDuration())});
//            }
//
//            // Increment to the next half-year.
//            if (halfYear == 1) {
//                halfYear = 2;
//            } else {
//                halfYear = 1;
//                year++;
//            }
//
//            // Update current to the start of the next half-year.
//            current = LocalDate.of(year, halfYear == 1 ? 1 : 7, 1);
//        }
//
//        return dates;
//    }

    private List<LocalDate[]> generateCustomHalfYearlyRecurringDates(RecurrenceScheduleDTO scheduleDTO, Long userAccountId) {
        List<LocalDate[]> dates = new ArrayList<>();
        LocalDate current = scheduleDTO.getStartDate();
        int year = current.getYear();
        int halfYear = current.getMonthValue() <= 6 ? 1 : 2; // Determine if the date is in the first or second half of the year.
        int occurrences = 0;
        int maxOccurrences = scheduleDTO.getNumberOfOccurrences() != null ? scheduleDTO.getNumberOfOccurrences() : Integer.MAX_VALUE;

        while ((scheduleDTO.getEndDate() == null || !current.isAfter(scheduleDTO.getEndDate())) && occurrences < maxOccurrences) {
            LocalDate targetDate = calculateTargetDateForHalfYear(scheduleDTO, year, halfYear, userAccountId);

            if (targetDate != null && !targetDate.isBefore(current) && (scheduleDTO.getEndDate() == null || !targetDate.isAfter(scheduleDTO.getEndDate()))) {
                dates.add(new LocalDate[]{targetDate, targetDate.plusDays(scheduleDTO.getOccurrenceDuration())});
                occurrences++;
            }

            // Increment to the next half-year.
            if (halfYear == 1) {
                halfYear = 2;
            } else {
                halfYear = 1;
                year++;
            }

            // Update current to the start of the next half-year.
            current = LocalDate.of(year, halfYear == 1 ? 1 : 7, 1);
        }

        return dates;
    }

    private LocalDate calculateTargetDateForHalfYear(RecurrenceScheduleDTO scheduleDTO, int year, int halfYear, Long userAccountId) {
        LocalDate targetDate;
        // Determine the target date based on the custom recurrence option.
        if (scheduleDTO.getCustomRecurrenceOption().getDayType() == RecurrenceScheduleDTO.DayType.DAY) {
            targetDate = findPositionalDayOfHalfYear(year, halfYear, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
        } else if (scheduleDTO.getCustomRecurrenceOption().getDayType() == RecurrenceScheduleDTO.DayType.WORKING_DAY) {
            targetDate = findPositionalBusinessDayOfHalfYear(year, halfYear, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name(), userAccountId);
        } else {
            DayOfWeek dayOfWeek = convertDayTypeToDayOfWeek(scheduleDTO.getCustomRecurrenceOption().getDayType());
            if (scheduleDTO.getCustomRecurrenceOption().getOrdinal() == RecurrenceScheduleDTO.Ordinal.FIRST || scheduleDTO.getCustomRecurrenceOption().getOrdinal() == RecurrenceScheduleDTO.Ordinal.SECOND) {
                targetDate = findSpecificDayOfHalfYear(year, halfYear, dayOfWeek, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
            } else {
                targetDate = findLastSpecificDayOfHalfYear(year, halfYear, dayOfWeek, scheduleDTO.getCustomRecurrenceOption().getOrdinal().name());
            }
        }
        return  targetDate;
    }


    /**
     * returns a particular day in the half year example first or last day of the half year
     */
    private LocalDate findPositionalDayOfHalfYear(int year, int halfYear, String position) {
        LocalDate startOfHalfYear = LocalDate.of(year, halfYear == 1 ? 1 : 7, 1);
        LocalDate endOfHalfYear = halfYear == 1 ? LocalDate.of(year, 6, 30) : LocalDate.of(year, 12, 31);

        switch (position) {
            case "First":
                return startOfHalfYear;
            case "Second":
                return startOfHalfYear.plusDays(1);
            case "Second Last":
                return endOfHalfYear.minusDays(1);
            case "Last":
                return endOfHalfYear;
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
    }


    /**
     * find the positional business day in the half-year
     */
    private LocalDate findPositionalBusinessDayOfHalfYear(int year, int halfYear, String position, Long userAccountId) {
        LocalDate date = findPositionalDayOfHalfYear(year, halfYear, position);
        HolidayOffDayInfo holidayOffDayInfo = getHolidaysAndOffDaysBasedOnEntityPreference(userAccountId);

        switch (position) {
            case "First":
            case "Second":
                while (!isBusinessDay(date, holidayOffDayInfo)) {
                    date = date.plusDays(1);
                }
                if ("Second".equals(position)) {
                    LocalDate nextDay = date.plusDays(1);
                    while (!isBusinessDay(nextDay, holidayOffDayInfo)) {
                        nextDay = nextDay.plusDays(1);
                    }
                    return nextDay; // Return the second business day.
                }
                return date; // Return the first business day.

            case "Second Last":
            case "Last":
                while (!isBusinessDay(date, holidayOffDayInfo)) {
                    date = date.minusDays(1);  // Move backward to find the last business day.
                }
                if ("Second Last".equals(position)) {
                    LocalDate previousDay = date.minusDays(1);
                    while (!isBusinessDay(previousDay, holidayOffDayInfo)) {
                        previousDay = previousDay.minusDays(1);  // Move backward to find the second last business day.
                    }
                    return previousDay; // Return the second last business day.
                }
                return date; // Return the last business day.

            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
    }


    /**
     * find first/ second specific positional day in the half-year example first Monday of the half-year
     */
    private LocalDate findSpecificDayOfHalfYear(int year, int halfYear, DayOfWeek day, String position) {
        LocalDate startOfHalfYear = LocalDate.of(year, halfYear == 1 ? 1 : 7, 1);
        LocalDate targetDate = startOfHalfYear.with(TemporalAdjusters.nextOrSame(day));

        if ("Second".equals(position)) {
            // Add 7 days to get the second occurrence of the day.
            targetDate = targetDate.plusWeeks(1);
        }

        return targetDate;
    }


    /**
     * find last/ second last specific positional day in the half-year example last Monday of the half-year
     */
    private LocalDate findLastSpecificDayOfHalfYear(int year, int halfYear, DayOfWeek day, String position) {
        LocalDate endOfHalfYear = halfYear == 1 ? LocalDate.of(year, 6, 30) : LocalDate.of(year, 12, 31);
        LocalDate targetDate = endOfHalfYear.with(TemporalAdjusters.previousOrSame(day));

        if ("Second Last".equals(position)) {
            // Subtract 7 days to get the second last occurrence of the day.
            targetDate = targetDate.minusWeeks(1);
        }

        return targetDate;
    }


    /**
     * gets holidays and off days values from entity preference
     */
    public HolidayOffDayInfo getHolidaysAndOffDaysBasedOnEntityPreference(Long userAccountId) {
        List<LocalDate> holidays = new ArrayList<>();
        List<Integer> offDays = new ArrayList<>();

        UserAccount userAccount = userAccountService.getActiveUserAccountByAccountId(userAccountId);
        EntityPreference orgPreference = entityPreferenceService.fetchEntityPreference(Constants.EntityTypes.ORG, userAccount.getOrgId());

        if (orgPreference != null) {
            holidays = orgPreference.getHolidayOffDays().stream().filter(HolidayOffDay::isActive)
                    .map(HolidayOffDay::getDate)
                    .collect(Collectors.toList());
            offDays = orgPreference.getOffDays();
        }

        return new HolidayOffDayInfo(holidays, offDays);
    }


    /**
     * checks whether the day is not a holiday or an off day in the week
     */
    public boolean isBusinessDay(LocalDate date, HolidayOffDayInfo holidayOffDayInfo) {
        List<LocalDate> holidays = holidayOffDayInfo.getHolidays();
        List<Integer> offDays = holidayOffDayInfo.getOffDays();
        return !holidays.contains(date) && !offDays.contains(date.getDayOfWeek().getValue());
    }


    /**
     * converts dayType to integer value of the day
     */
    private DayOfWeek convertDayTypeToDayOfWeek(RecurrenceScheduleDTO.DayType dayType) {
        return DayOfWeek.valueOf(dayType.name());
    }


    /**
     * gets quarter value from date
     */
    private int getQuarter(LocalDate date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }


}
