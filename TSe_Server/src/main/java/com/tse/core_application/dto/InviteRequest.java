package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InviteRequest {

    @Size(max=50)
    @NotBlank(message = ErrorConstant.InviteError.FIRST_NAME)
    private String firstName;

    @Size(max=50)
    private String middleName;

    @Size(max=50)
    @NotBlank(message = ErrorConstant.InviteError.LAST_NAME)
    private String lastName;

    @Size(max=70)
    @NotBlank(message = ErrorConstant.InviteError.EMAIL)
    @Email(message = ErrorConstant.InviteError.EMAIL_FORMAT)
    private String primaryEmail;

    @NotNull(message = ErrorConstant.InviteError.ENTITY_TYPE_ID)
    private Integer entityTypeId;

    @NotNull(message = ErrorConstant.InviteError.ENTITY_ID)
    private Long entityId;

    @NotNull(message = ErrorConstant.InviteError.SENT_DATE)
    private LocalDate userLocalSentDate; // User's local date when the invite was sent

    @NotNull(message = ErrorConstant.InviteError.VALIDITY_DURATION_REQUIRED)
    @Min(value = 0, message = ErrorConstant.InviteError.VALIDITY_DURATION_RANGE)
    @Max(value = 14, message = ErrorConstant.InviteError.VALIDITY_DURATION_RANGE)
    private Integer validityDuration; // number of days the invite is valid
}
