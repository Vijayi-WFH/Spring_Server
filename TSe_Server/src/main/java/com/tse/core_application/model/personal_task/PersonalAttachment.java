package com.tse.core_application.model.personal_task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.model.Constants;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_task_attachment", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PersonalAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id", nullable = false, unique = true)
    private Long attachmentId;

    @Column(name = "personal_task_id", nullable = false)
    private Long personalTaskId;

    @Column(name = "file_name", nullable = false, length = 500)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(max= 100)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Double fileSize;

    @Column(name = "file_content", nullable = false)
    @Lob
    @JsonIgnore
    private byte[] fileContent;

    @Column(name = "file_status", nullable = false, length = 1)
    private Character fileStatus;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private LocalDateTime createdDateTime;

    @Column(name = "deleted_date_time")
    private LocalDateTime deletedDateTime;
}
