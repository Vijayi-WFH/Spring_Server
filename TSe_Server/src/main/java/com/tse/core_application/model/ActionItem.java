package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
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
@Table(name = "action_item", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ActionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_item_id")
    private Long actionItemId;

    @Column(name = "action_item",length=4000)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(max=500, message= ErrorConstant.ActionItem.ACTION_ITEM)
    private String actionItem;

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

    @Column(name = "is_important", nullable = false)
    private Boolean isImportant = false;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Transient
    private Boolean isUpdated;

    @Column(name = "task_id")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", referencedColumnName = "meeting_id", nullable = false)
    @JsonBackReference
    private Meeting meeting;

}
