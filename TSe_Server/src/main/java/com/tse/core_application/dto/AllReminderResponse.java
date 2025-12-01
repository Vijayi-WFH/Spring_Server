package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AllReminderResponse {
    private List<ReminderResponse> todayReminders = new ArrayList<>();
    private List<ReminderResponse> futureReminders = new ArrayList<>();
    private List<ReminderResponse> pastReminders = new ArrayList<>();
}
