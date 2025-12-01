package com.tse.core_application.dto;

import com.tse.core_application.dto.MeetingResponse;
import com.tse.core_application.model.Meeting;
import com.tse.core_application.model.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Task task;
    private List<MeetingResponse> meetingList;

}
