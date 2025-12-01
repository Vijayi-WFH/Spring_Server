package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "meeting_sequence", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MeetingSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_sequence_id")
    private Long meetingSequenceId;

    @Column(name = "last_meeting_identifier", nullable = false)
    private Long lastMeetingIdentifier; // indicates the last meeting number (numeric) used in the given organization

    @Column(name = "org_id", nullable = false)
    private Long orgId;


    public MeetingSequence(Long orgId, Long lastMeetingIdentifier) {
        this.lastMeetingIdentifier = lastMeetingIdentifier;
        this.orgId = orgId;
    }
}
