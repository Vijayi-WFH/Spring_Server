package com.tse.core_application.custom.model;


import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.validators.annotations.NotNullOrZero;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewEffortTrack {
	@NotNullOrZero(message = ErrorConstant.Task.NEW_EFFORT)
	private Integer newEffort;
	@NotNull(message = ErrorConstant.Task.NEW_EFFORT_DATE)
    private LocalDate newEffortDate;
}
