package com.tse.core_application.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "personal_task_sequence", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PersonalTaskSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_task_sequence_id")
    private Long personalTaskSequenceId;

    @Column(name = "last_task_identifier", nullable = false)
    private Long lastTaskIdentifier; // indicates the last task number used in the given team

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    public PersonalTaskSequence(Long accountId, Long lastTaskIdentifier) {
        this.lastTaskIdentifier = lastTaskIdentifier;
        this.accountId = accountId;
    }
}
