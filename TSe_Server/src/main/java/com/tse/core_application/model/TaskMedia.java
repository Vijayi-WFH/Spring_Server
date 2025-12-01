package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Table(name = "task_media", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_media_id", nullable = false)
    private Long taskMediaId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "task_number", nullable = false, length = 40)
    private String taskNumber;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "media", nullable = false)
    private byte[] media;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_name", nullable = false)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(max=50)
    private String fileName;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;

}
