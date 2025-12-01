package com.tse.core_application.dto;

import com.tse.core_application.model.Constants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class HolidayRequest {

        private Long holidayId;

        @NotNull(message = Constants.EntityPreference.DATE_NOT_NULL)
        private LocalDate date;

//        private boolean isRecurring;

        @NotNull(message = Constants.EntityPreference.DESCRIPTION_NOT_NULL)
        private String description;

        private Boolean isToDelete;
}
