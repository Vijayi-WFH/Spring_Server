package com.tse.core_application.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.utils.EmptyStringToNullDeserializer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UserNameChangeRequest {
    @NonNull
    private Long accountId;
    @NotNull(message = ErrorConstant.FIRST_NAME_ERROR)
    @Size(min = 1, max = 50, message = ErrorConstant.FIRST_NAME_LENGTH)
    private String firstName;

    @JsonDeserialize(using = EmptyStringToNullDeserializer.class)
    @Size(min = 1, max = 50, message = ErrorConstant.MIDDLE_NAME_LENGTH)
    private String middleName;
    @NotNull(message = ErrorConstant.LAST_NAME_ERROR)
    @Size(min = 1, max = 50, message = ErrorConstant.LAST_NAME_LENGTH)
    private String lastName;
}
