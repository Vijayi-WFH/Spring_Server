package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "epic_sequence", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EpicSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "epic_sequence_id")
    private Long epicSequenceId;

    @Column(name = "last_epic_identifier", nullable = false)
    private Long lastEpicIdentifier;

    @Column(name = "project_id", nullable = false)
    private Long projectId;


    public EpicSequence(Long projectId, Long lastEpicIdentifier) {
        this.lastEpicIdentifier = lastEpicIdentifier;
        this.projectId = projectId;
    }
}
