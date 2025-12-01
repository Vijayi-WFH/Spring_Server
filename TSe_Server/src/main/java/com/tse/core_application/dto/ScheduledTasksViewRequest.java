package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduledTasksViewRequest {

        @Nullable
        private Long orgId;

        @Nullable
        private Long buId;

        @Nullable
        private  Long projectId;

        @Nullable
        private Long teamId;
}
