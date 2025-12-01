package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "recurring_meeting_sequence", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecurringMeetingSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurring_meeting_sequence_id")
    private Long recurringMeetingSequenceId;

    @Column(name = "last_recurring_meeting_identifier", nullable = false)
    private Long lastRecurringMeetingIdentifier; // indicates the last recurring meeting number (numeric) used in the given organization

    @Column(name = "org_id", nullable = false)
    private Long orgId;


    public RecurringMeetingSequence (Long orgId, Long lastRecurringMeetingIdentifier) {
        this.lastRecurringMeetingIdentifier = lastRecurringMeetingIdentifier;
        this.orgId = orgId;
    }
}
