package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DateRequest {

    @NotNull(message = ErrorConstant.RecurTask.SELECTED_DATE)
    private LocalDateTime todaysDate;
}
