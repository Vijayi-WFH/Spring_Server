package com.tse.core_application.dto;

import com.tse.core_application.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MemberDetailsTeam {

//    @NotNull(message = "Work status is required")
    private Constants.WorkStatus workStatus;

//    @NotNull(message = "Work minutes are required")
    @Min(value = 1, message = "Work minutes must be greater than 0")
    private Integer workMinutes;
}
