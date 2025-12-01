package com.tse.core_application.dto;

import com.tse.core_application.custom.model.MeetingDetails;
import com.tse.core_application.custom.model.TaskMaster;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodayFocusResponse {

    List<TaskMaster> toDoTaskList;
    List<MeetingDetails> meetingResponseList;
    List<TaskMaster> delayedTaskList;
    List<TaskMaster> watchlistTaskList;
    List<TaskMaster> taskWithDependencies;

}
