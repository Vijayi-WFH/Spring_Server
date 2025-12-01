package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "sprint_capacity_metrics", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SprintCapacityMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sprint_capacity_metrics_id")
    private Long sprintCapacityMetricsId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "sprint_id", nullable = false)
    private Long sprintId;

    @Column(name = "total_capacity")
    private Double totalCapacity; // in minutes

    @Column(name = "current_planned_capacity")
    private Integer currentPlannedCapacity = 0; // in minutes

    @Column(name = "percent_capacity_utilization")
    private Integer percentPlannedCapacityUtilization = 0;

    @Column(name = "loaded_capacity")
    private Integer loadedCapacity = 0; // in minutes

    @Column(name = "percent_loaded_capacity_utilization")
    private Integer percentLoadedCapacityUtilization = 0;

    @Column(name = "totalWorkingDays")
    private Double totalWorkingDays;

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

    @Column(name = "moved_capacity")
    private Integer movedCapacity;

}


