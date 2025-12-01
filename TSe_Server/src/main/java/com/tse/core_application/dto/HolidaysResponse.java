package com.tse.core_application.dto;

import com.tse.core_application.model.HolidayOffDay;
import lombok.Data;

import java.util.List;

@Data
public class HolidaysResponse {
    private List<HolidayOffDay> nextTwoHolidays;
    private List<HolidayOffDay> allHolidays;
}
