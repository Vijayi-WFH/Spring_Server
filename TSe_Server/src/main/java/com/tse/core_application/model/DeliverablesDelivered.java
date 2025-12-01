package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
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
@Table(name = "deliverables_delivered", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeliverablesDelivered {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deliverables_delivered_log_id")
    private Long deliverablesDeliveredLogId;

    @Column(name = "deliverables_delivered", length=1000)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(max=255)
    private String deliverablesDelivered;

    @Column(name = "list_of_deliverables_delivered_id")
    private Long listOfDeliverablesDeliveredId;

    @Column(name = "created_by_account_id", nullable = false)
    private Long createdByAccountId;

    @Column(name = "updated_by_account_id")
    private Long updatedByAccountId;

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
