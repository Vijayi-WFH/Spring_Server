package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendeeRequest {


    @Nullable
    private Long attendeeLogId;
    @NotNull(message="accountId cannot be null.")
    private Long accountId;

    // added below 3 fields in task 2676
    private Long buId;
    private Long projectId;
    private Long teamId;


    //private Integer attendeeInvitationStatusId;
    @Nullable
    private Integer isAttendeeExpected;
    private Long systemGenEfforts;

}
