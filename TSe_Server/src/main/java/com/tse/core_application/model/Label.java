package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "label", schema = Constants.SCHEMA_NAME)
//uniqueConstraints = @UniqueConstraint(columnNames = {"label_name", "team_id"}
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "label_id")
    private Long labelId;

    @NotNull(message = ErrorConstant.LABEL_NAME)
    @Size(min = 2, max = 30, message = ErrorConstant.LABEL_NAME_SIZE)
    @Column(name = "label_name", nullable = false)
    private String labelName;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "labels")
    @JsonIgnore
    private List<Task> tasks = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "meetingLabels")
    @JsonIgnore
    private List<Meeting> meetings = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "recurMeetingLabels")
    @JsonIgnore
    private List<RecurringMeeting> recurringMeetings = new ArrayList<>();

//    @ManyToOne(fetch = FetchType.LAZY)
////    @JsonBackReference
//    @JsonIgnoreProperties({"labels", "tasks"})
//    @JoinColumn(name = "team_id", referencedColumnName = "team_id")
//    private Team team;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId;
}