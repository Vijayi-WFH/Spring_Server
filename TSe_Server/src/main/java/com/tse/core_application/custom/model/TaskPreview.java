package com.tse.core_application.custom.model;

import lombok.Data;
import org.springframework.lang.Nullable;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TaskPreview {
    private Long version;
    private LocalDateTime lastUpdatedDateTime;
    private List<Long> childTaskIds;
    private List<Long> deletedChildTaskIds;
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.StatType taskProgressSystem;
    private LocalDateTime taskProgressSystemLastUpdated;
    private List<Long> dependencyIds;
    private Integer taskEstimate;
    private LocalDateTime nextTaskProgressSystemChangeDateTime;
    private List<Long> meetingList = new ArrayList<>();
}
