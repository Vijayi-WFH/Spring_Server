package com.tse.core_application.dto.capacity;

import com.tse.core_application.dto.ReferenceMeetingDetail;
import com.tse.core_application.model.StatType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CapacityTaskDetails {
    private Long taskId;
    private String taskNumber;
    private Long teamId;
    private String taskTitle;
    private LocalDateTime taskActStDate;
    private LocalDateTime taskActEndDate;
    private LocalDateTime taskExpStartDate;
    private LocalDateTime taskExpEndDate;
    private Integer currentActivityIndicator;
    private Boolean currentlyScheduledTaskIndicator;
    private StatType taskProgressSystem;
    private String taskPriority;
    private Integer taskEstimate;
    private String workflowTaskStatus;
    private Integer meetingEffortPreferenceId;
    private List<ReferenceMeetingDetail> referenceMeetingList;
    private Integer taskTypeId;
    private Boolean isBug;
}
