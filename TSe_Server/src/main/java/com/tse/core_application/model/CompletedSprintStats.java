package com.tse.core_application.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.dto.ProgressSystemSprintTask;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "completed_sprint_stats", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompletedSprintStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "completed_sprint_stats_id")
    private Long completedSprintStatsId;

    @Column(name = "sprint_id", nullable = false)
    private Long sprintId;

    @Column(name = "not_started_tasks")
    private Integer notStartedTasks;

    @Column(name = "watchlist_tasks")
    private Integer watchListTasks;

    @Column(name = "on_track_tasks")
    private Integer onTrackTasks;

    @Column(name = "completed_tasks")
    private Integer completedTasks;

    @Column(name = "delayed_tasks")
    private Integer delayedTasks;

    @Column(name = "total_tasks")
    private Integer totalTasks;

    @Column(name = "deleted_tasks")
    private Integer deletedTasks;

    @Column(name = "late_completed_tasks")
    private Integer lateCompletedTasks;

    @Column(name = "not_started_task_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String notStartedTasksList;

    @Column(name = "watchlist_task_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String watchListTasksList;

    @Column(name = "on_track_task_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String onTrackTasksList;

    @Column(name = "delayed_task_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String delayedTasksList;

    @Column(name = "completed_task_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String completedTasksList;

    @Column(name = "late_completed_task_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String lateCompletedTasksList;

    @Column(name = "deleted_task_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String deletedTasksList;

    public List<ProgressSystemSprintTask> getNotStartedTasksList() {
        return convertJsonToList(notStartedTasksList);
    }

    public void setNotStartedTasksList(List<ProgressSystemSprintTask> notStartedTasksList) {
        this.notStartedTasksList = convertListToJson(notStartedTasksList);
    }

    public List<ProgressSystemSprintTask> getWatchListTasksList() {
        return convertJsonToList(watchListTasksList);
    }

    public void setWatchListTasksList(List<ProgressSystemSprintTask> watchListTasksList) {
        this.watchListTasksList = convertListToJson(watchListTasksList);
    }

    public List<ProgressSystemSprintTask> getOnTrackTasksList() {
        return convertJsonToList(onTrackTasksList);
    }

    public void setOnTrackTasksList(List<ProgressSystemSprintTask> onTrackTasksList) {
        this.onTrackTasksList = convertListToJson(onTrackTasksList);
    }

    public List<ProgressSystemSprintTask> getDelayedTasksList() {
        return convertJsonToList(delayedTasksList);
    }

    public void setDelayedTasksList(List<ProgressSystemSprintTask> delayedTasksList) {
        this.delayedTasksList = convertListToJson(delayedTasksList);
    }

    public List<ProgressSystemSprintTask> getCompletedTasksList() {
        return convertJsonToList(completedTasksList);
    }

    public void setCompletedTasksList(List<ProgressSystemSprintTask> completedTasksList) {
        this.completedTasksList = convertListToJson(completedTasksList);
    }

    public List<ProgressSystemSprintTask> getLateCompletedTasksList() {
        return convertJsonToList(lateCompletedTasksList);
    }

    public void setLateCompletedTasksList(List<ProgressSystemSprintTask> lateCompletedTasksList) {
        this.lateCompletedTasksList = convertListToJson(lateCompletedTasksList);
    }

    public List<ProgressSystemSprintTask> getDeletedTasksList() {
        return convertJsonToList(deletedTasksList);
    }

    public void setDeletedTasksList(List<ProgressSystemSprintTask> deletedTasksList) {
        this.deletedTasksList = convertListToJson(deletedTasksList);
    }

    private List<ProgressSystemSprintTask> convertJsonToList(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // Ensure that it doesn't fail if there are unknown properties
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json, new TypeReference<List<ProgressSystemSprintTask>>() {});
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String convertListToJson(List<ProgressSystemSprintTask> list) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error while serializing task list to JSON", e);
        }
    }

}
