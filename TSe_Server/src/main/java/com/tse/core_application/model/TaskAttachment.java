package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_attachment", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_attachment_id", nullable = false, unique = true)
    private Long taskAttachmentId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "file_name", nullable = false, length = 500)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(max= 100)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Double fileSize;

//    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_content", nullable = false)
    @Lob
    @JsonIgnore
    private byte[] fileContent;

    @Column(name = "file_status", nullable = false, length = 1)
    private Character fileStatus;

    @Column(name = "uploader_account_id", nullable = false)
    private Long uploaderAccountId;

    @Column(name = "remover_account_id")
    private Long removerAccountId;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_log_id")
    @JsonBackReference
    private Comment comment;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private LocalDateTime createdDateTime;

//    @UpdateTimestamp
    @Column(name = "deleted_date_time")
    private LocalDateTime deletedDateTime;
}
