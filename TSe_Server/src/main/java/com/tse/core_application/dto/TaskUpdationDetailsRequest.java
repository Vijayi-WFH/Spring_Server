package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdationDetailsRequest {

    @NotNull(message = ErrorConstant.TaskUpdationDetails.ACCOUNT_ID)
    List<Long> accountIdList;

    @NotNull(message = ErrorConstant.TaskUpdationDetails.NOTIFICATION_TYPE_ID)
    List<String>notificationTypeIdList;

    @NotNull(message = ErrorConstant.TaskUpdationDetails.ENTITY_TYPE_ID)
    Integer entityTypeId;

    @NotNull(message = ErrorConstant.TaskUpdationDetails.ENTITY_ID)
    Long entityId;

    LocalDate fromDate;
    LocalDate toDate;
    Boolean sortByName;
    Boolean sortByTaskNumber;
}
