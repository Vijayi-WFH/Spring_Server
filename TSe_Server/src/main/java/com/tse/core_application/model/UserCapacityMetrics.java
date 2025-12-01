package com.tse.core_application.model;

import javax.persistence.*;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_capacity_metrics", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserCapacityMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_capacity_metrics_id")
    private Long userCapacityMetricsId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "sprint_id", nullable = false)
    private Long sprintId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "total_capacity")
    private Double totalCapacity = 0.0; // in minutes

    @Column(name = "loaded_capacity_ratio", nullable = false)
    private Double loadedCapacityRatio = 1.0; // By default, 1, value lies between 0 and 1

    @Column(name = "loaded_capacity")
    private Integer loadedCapacity = 0;

    @Column(name = "current_planned_capacity")
    private Integer currentPlannedCapacity = 0 ; // in minutes

    @Column(name = "percent_planned_utilization")
    private Integer percentPlannedCapacityUtilization = 0;

    @Column(name = "percent_loaded_planned_utilization")
    private Integer percentLoadedCapacityUtilization = 0;

    @Column(name = "totalWorkingDays")
    private Double totalWorkingDays = 0.0;

    @Column(name = "workMinutes")
    private Integer workMinutes = 0;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "burned_efforts")
    private Integer burnedEfforts;

    @Column(name = "total_earned_efforts")
    private Integer totalEarnedEfforts;

    @Column(name = "earned_efforts_task")
    private Integer earnedEffortsTask;

    @Column(name = "earned_efforts_meeting")
    private Integer earnedEffortsMeeting;

    @Column(name = "minutes_behind_schedule")
    private Integer minutesBehindSchedule;

    @Column(name = "is_removed")
    private Boolean isRemoved = false;
}
