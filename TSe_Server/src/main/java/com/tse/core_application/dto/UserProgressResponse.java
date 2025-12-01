package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProgressResponse {
    private List<SprintProgress> sprintProgress;
    private Integer sprintTotalHours = 0;
    private Integer consumedSprintHours = 0;
    private List<TaskProgress> taskProgress;
    private Integer taskTotalHours = 0;
    private Integer consumedTaskHours = 0;
    private List<MeetingProgress> meetingProgress;
    private Integer meetingTotalHours = 0;
    private Integer consumedMeetingHours = 0;
}
