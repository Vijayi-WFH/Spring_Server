package com.tse.core_application.model.performance_notes;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.model.Constants;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_rating", schema = Constants.SCHEMA_NAME)
@Data
public class TaskRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_rating_id", nullable = false, unique = true)
    private Integer taskRatingId;

    @Column(name = "task_rating_desc", length = 1000, nullable = false)
    private String taskRatingDesc;

    @Column(name = "task_rating", nullable = false)
    private Integer taskRating;

    @Column(name = "task_rating_display_as")
    private String taskRatingDisplayAs;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

}
