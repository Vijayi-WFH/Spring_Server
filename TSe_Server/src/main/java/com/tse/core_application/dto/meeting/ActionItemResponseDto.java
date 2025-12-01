package com.tse.core_application.dto.meeting;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Meeting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ActionItemResponseDto {
    private Long actionItemId;
    private String actionItem;
    private Long postedByAccountId;
    private Long modifiedByAccountId;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;
    private Boolean isImportant;
    private Long version;
    private Boolean isDeleted;
    private Boolean isUpdated;
    private Long taskId;
    private WorkItemProgressDetailsDto taskDetails;
}
