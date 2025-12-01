package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProgressRequest {
    private Long orgId;
    private Long teamId;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
