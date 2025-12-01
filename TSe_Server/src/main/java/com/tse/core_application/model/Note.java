package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.validators.CleanedSize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "note", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_log_id")
    private Long noteLogId;

    @Column(name = "note",length=2500)
    @Convert(converter = DataEncryptionConverter.class)
//    @Size(max=500)
    @CleanedSize(value = 500, message = "Note must not exceed 500 characters")
    private String note;

    @Column(name = "note_id")
    private Long noteId;

    @Column(name = "posted_by_account_id", nullable = false)
    private Long postedByAccountId;

    @Column(name = "modified_by_account_id")
    private Long modifiedByAccountId;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "is_deleted")
    private Integer isDeleted;

    @Transient
    private Integer isUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "task_id", nullable = false)
    @JsonBackReference
    private Task task;


}
