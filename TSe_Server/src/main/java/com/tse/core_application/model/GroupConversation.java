package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.utils.LongListConverter;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_conversation", schema = Constants.SCHEMA_NAME)
@Data
public class GroupConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_conversation_id", nullable = false)
    private Long groupConversationId;

    @Column(name = "message", nullable = false, length=20000)
//    @Size(max = 1000, message = ErrorConstant.Task.MESSAGE_LIMIT)
    @Convert(converter = DataEncryptionConverter.class)
    private String message;

    @Column(name = "message_tags", columnDefinition = "varchar[]")
    @Type(type = "com.tse.core_application.utils.CustomStringArrayUserTypesEntity")
    private String[] messageTags;

    @Column(name = "posted_by_account_id", nullable = false)
    private Long postedByAccountId;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "parent_group_conversation_id")
    private Long parentGroupConversationId;

    @Column(name = "child_gc_ids")
    @Convert(converter = LongListConverter.class)
    private List<Long> childGcIds = new ArrayList<>();

    @OneToMany(mappedBy = "groupConversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Attachment> attachments;
}
