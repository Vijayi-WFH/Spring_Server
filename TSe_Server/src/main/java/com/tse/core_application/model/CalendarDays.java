package com.tse.core_application.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import javax.persistence.Table;


@Entity
@Data
@Table(name = "calendar_days", schema = Constants.SCHEMA_NAME)
public class CalendarDays {

    @Id
    @Column(name = "calendar_date_num")
    private Integer calendarDateNum;

    @NotNull
    @Column(name = "calendar_date")
    private LocalDate calendarDate;

    @NotNull
    @Column(name = "calendar_date_string", length=10)
    private String calendarDateString;

    @NotNull
    @Column(name = "calendar_month")
    private Integer calendarMonth;

    @NotNull
    @Column(name = "calendar_day")
    private Integer calendarDay;

    @NotNull
    @Column(name = "calendar_year")
    private Integer calendarYear;

    @NotNull
    @Column(name = "calendar_quarter")
    private Integer calendarQuarter;

    @NotNull
    @Column(name = "day_name", length = 9)
    private String dayName;

    @NotNull
    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @NotNull
    @Column(name = "day_of_week_in_month")
    private Integer dayOfWeekInMonth;

    @NotNull
    @Column(name = "day_of_week_in_year")
    private Integer dayOfWeekInYear;

    @NotNull
    @Column(name = "day_of_week_in_quarter")
    private Integer dayOfWeekInQuarter;

    @NotNull
    @Column(name = "day_of_quarter")
    private Integer dayOfQuarter;

    @NotNull
    @Column(name = "day_of_year")
    private Integer dayOfYear;

    @NotNull
    @Column(name = "week_of_month")
    private Integer weekOfMonth;

    @NotNull
    @Column(name = "week_of_quarter")
    private Integer weekOfQuarter;

    @NotNull
    @Column(name = "week_of_year")
    private Integer weekOfYear;

    @NotNull
    @Column(name = "month_name", length = 9)
    private String monthName;

    @NotNull
    @Column(name = "first_date_of_week")
    private LocalDate firstDateOfWeek;

    @NotNull
    @Column(name = "last_date_of_week")
    private LocalDate lastDateOfWeek;

    @NotNull
    @Column(name = "first_date_of_month")
    private LocalDate firstDateOfMonth;

    @NotNull
    @Column(name = "last_date_of_month")
    private LocalDate lastDateOfMonth;

    @NotNull
    @Column(name = "first_date_of_quarter")
    private LocalDate firstDateOfQuarter;

    @NotNull
    @Column(name = "last_date_of_quarter")
    private LocalDate lastDateOfQuarter;

    @NotNull
    @Column(name = "is_business_day")
    private Boolean isBusinessDay;

    @Column(name = "previous_business_day")
    private LocalDate previousBusinessDay;

    @Column(name = "next_business_day")
    private LocalDate nextBusinessDay;

    @NotNull
    @Column(name = "is_leap_year")
    private Boolean isLeapYear;

    @NotNull
    @Column(name = "days_in_month")
    private Integer daysInMonth;
}


