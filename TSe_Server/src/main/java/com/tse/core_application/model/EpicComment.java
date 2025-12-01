package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.utils.LongListConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "epic_comment", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EpicComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_log_id", nullable = false)
    private Long commentLogId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "comment", nullable = false, length=5000)
    @Convert(converter = DataEncryptionConverter.class)
    private String comment;

    @Column(name = "comments_tags", columnDefinition = "varchar[]")
    @Type(type = "com.tse.core_application.utils.CustomStringArrayUserTypesEntity")
    private String[] commentsTags;

    @Column(name = "posted_by_account_id", nullable = false)
    private Long postedByAccountId;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;

    @Column(name = "parent_comment_log_id")
    private Long parentCommentLogId;

    @Column(name = "child_comment_log_ids")
    @Convert(converter = LongListConverter.class)
    private List<Long> childCommentLogIds = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epic_id", referencedColumnName = "epic_id")
    @JsonBackReference
    private Epic epic;
}
