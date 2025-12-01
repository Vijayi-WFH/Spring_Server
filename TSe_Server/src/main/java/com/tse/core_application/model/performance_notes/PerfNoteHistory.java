package com.tse.core_application.model.performance_notes;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.UserAccount;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "perf_note_history", schema = Constants.SCHEMA_NAME)
@Data
public class PerfNoteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "perfnote_history_log_id", nullable = false, unique = true)
    private Long perfNoteHistoryLogId;

    @Column(name = "perf_not_id", nullable = false)
    private Long perfNoteId;

    @Column(name = "perf_note", length = 20000)
    @Convert(converter = DataEncryptionConverter.class)
    private String perfNote;

    @ManyToOne(optional = false)
    @JoinColumn(name = "posted_by_account_id", referencedColumnName = "account_id")
    private UserAccount fkPostedByAccountId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_to_account_id", referencedColumnName = "account_id")
    private UserAccount fkAssignedToAccountId;

    @Column(name = "is_shared")
    private Boolean isShared;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @ManyToOne
    @JoinColumn(name = "task_rating_id", referencedColumnName = "task_rating_id")
    private TaskRating fkTaskRatingId;

    @ManyToOne
    @JoinColumn(name = "modified_by_account_id", referencedColumnName = "account_id")
    private UserAccount fkModifiedByAccountId;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @Column(name = "version")
    private int version;
}
