package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_attachment_history", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskAttachmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_attachment_history_id", nullable = false, unique = true)
    private Long taskAttachmentHistoryId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "file_name", nullable = false, length = 1000)
    @Convert(converter = DataEncryptionConverter.class)
    private String fileName;

    @Column(name = "is_file_added", nullable = false)
    private Boolean isFileAdded;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id_last_updated", referencedColumnName = "account_id")
    private UserAccount fkAccountIdLastUpdated;

    @Column(name = "version")
    private Long version;

    public TaskAttachmentHistory (Long taskId, String fileName, Boolean isFileAdded, LocalDateTime modifiedDate, UserAccount fkAccountIdLastUpdated, Long version) {
        this.taskId = taskId;
        this.fileName = fileName;
        this.isFileAdded = isFileAdded;
        this.modifiedDate = modifiedDate;
        this.fkAccountIdLastUpdated = fkAccountIdLastUpdated;
        this.version = version;
    }
}
