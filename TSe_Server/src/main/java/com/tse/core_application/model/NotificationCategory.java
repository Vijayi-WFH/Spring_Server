package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notification_category", schema = Constants.SCHEMA_NAME)
public class NotificationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "category_type", nullable = false)
    private String categoryType;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "category_desc", nullable = false)
    private String categoryDesc;

    @Column(name = "retention_days", nullable = false)
    private Integer retentionDays;

    @CreationTimestamp
    @Column(name = "created_date_time")
    private LocalDateTime createdDateTime;
}
