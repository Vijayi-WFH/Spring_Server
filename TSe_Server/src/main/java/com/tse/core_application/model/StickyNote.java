package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.utils.StringListConverter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="sticky_note", schema=Constants.SCHEMA_NAME)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class StickyNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id", nullable = false, unique = true)
    private Long noteId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "posted_by_account_id")
    private Long postedByAccountId;

    @Column(name = "modified_by_account_id")
    private Long modifiedByAccountId;

    @Column(name = "note", length = 15000)
    @Convert(converter = DataEncryptionConverter.class)
    private String note;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "bu_id")
    private Long buId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "shared_account_ids")
    @Convert(converter = StringListConverter.class)
    private List<String> sharedAccountIds;

    @Column(name = "access_type")
    private Integer accessType;

    @Column(name = "is_deleted")
    private Integer isDeleted;

    @Column(name = "share_edit_allowed")
    private Boolean shareEditAllowed = false;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

}
