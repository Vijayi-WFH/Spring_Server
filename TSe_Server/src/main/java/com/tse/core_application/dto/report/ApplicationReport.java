package com.tse.core_application.dto.report;

import lombok.Data;

@Data
public class ApplicationReport {
    private Long totalOrganization;
    private Long totalBu;
    private Long totalProject;
    private Long totalTeam;
    private Long totalUser;
    private Long totalEpics;
    private Long totalSprints;
    private Long totalTask;
    private Long totalNotes;
    private Long totalComments;
    private Long totalTemplates;
    private Long totalMeetings;
    private Long totalStickyNotes;
    private Long totalLeaves;
    private Long totalFeedback;
    private Long totalConversations;
}
