package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class SignUpCompletionDetail {

	@NotNull(message = ErrorConstant.HIGHEST_EDUCATION_ERROR)
    private Integer highestEducation;

    @NotNull(message = ErrorConstant.GENDER_ERROR)
    private Integer gender;

    @NotNull(message = ErrorConstant.AGE_RANGE_ERROR)
    private Integer ageRange;

    @NotNull(message = ErrorConstant.CITY)
    private String city;
}
