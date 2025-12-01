package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_note", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MeetingNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_note_id")
    private Long meetingNoteId;

    @Column(name = "meeting_note", length = 10000)
    @Convert(converter = DataEncryptionConverter.class)
//    @Size(max = 1000, message = ErrorConstant.MeetingNote.MEETING_NOTE)
    private String meetingNote;

    @Column(name = "posted_by_account_id", nullable = false)
    private Long postedByAccountId;

    @Column(name = "modified_by_account_id")
    private Long modifiedByAccountId;

    @Column(name = "is_important", nullable = false)
    private Boolean isImportant = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "updated_date_time")
    private LocalDateTime updatedDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", referencedColumnName = "meeting_id", nullable = false)
    @JsonBackReference
    private Meeting meeting;
}

