package com.tse.core_application.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "task_sequence", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_sequence_id")
    private Long taskSequenceId;

    @Column(name = "last_task_identifier", nullable = false)
    private Long lastTaskIdentifier; // indicates the last task number used in the given team

    @Column(name = "team_id", nullable = false)
    private Long teamId;


    public TaskSequence(Long teamId, Long lastTaskIdentifier) {
        this.lastTaskIdentifier = lastTaskIdentifier;
        this.teamId = teamId;
    }
}
