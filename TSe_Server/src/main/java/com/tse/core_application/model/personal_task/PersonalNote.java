package com.tse.core_application.model.personal_task;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.model.Constants;
import com.tse.core_application.validators.CleanedSize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_note", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PersonalNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @Column(name = "note",length=2500)
    @Convert(converter = DataEncryptionConverter.class)
    @CleanedSize(value = 500, message = "Note must not exceed 500 characters")
    private String note;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_task_id", referencedColumnName = "personal_task_id", nullable = false)
    @JsonBackReference
    private PersonalTask personalTask;

}