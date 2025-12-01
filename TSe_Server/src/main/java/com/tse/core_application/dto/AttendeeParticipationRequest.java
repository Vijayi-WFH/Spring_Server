package com.tse.core_application.dto;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendeeParticipationRequest {
    @NotNull(message="accountId cannot be null.")
    private Long accountId;
    @NotNull(message="meetingId cannot be null")
    private Long meetingId;

    private Integer isAttendeeExpected; // new field added
    private Integer didYouAttend;
    private Integer attendeeDuration;
    private LocalDateTime initialEffortDateTime;
    private Integer userPerceivedPercentageTaskCompleted; //required when assignee effort in meeting is billed

}
