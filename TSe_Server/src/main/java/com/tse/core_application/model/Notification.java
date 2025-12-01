package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.utils.LongListConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notification", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false, unique = true)
    private Long notificationId;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "notification_type_id", referencedColumnName = "notification_type_id")
    private NotificationType notificationTypeID;

    @ManyToOne
    @JoinColumn(name = "org_id", referencedColumnName = "org_id")
    private Organization orgId;

    @Column(name = "bu_id")
    private Long buId;

    @ManyToOne
    @JoinColumn(name = "project_id", referencedColumnName = "project_id")
    private Project projectId;

    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "team_id")
    private Team teamId;

    @ManyToOne
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private UserAccount accountId;


    //for task
    @Column(name = "task_number", length = 40)
    private String taskNumber;

    //for meeting
    @Column(name = "meeting_id")
    private Long meetingId;

    //for leave
    @Column(name = "leave_application_id")
    private Long leaveApplicationId;

    @NotNull
    @Column(name = "notification_title",length = 1000)
    @Convert(converter = DataEncryptionConverter.class)
    private String notificationTitle;

    @NotNull
    @Column(name = "notification_body",length = 2500)
    @Convert(converter = DataEncryptionConverter.class)
    private String notificationBody;

    @Column(name = "category_id")
    private Integer categoryId;

    @NotNull
    @Column(name = "payload",length = 5000)
    @Convert(converter = DataEncryptionConverter.class)
    private String payload;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @ManyToOne
    @JoinColumn(name = "notification_creator_account_id", referencedColumnName = "account_id")
    private UserAccount notificationCreatorAccountId;

    @Convert(converter = LongListConverter.class)
    @Column(name = "tagged_account_ids", columnDefinition = "TEXT")
    private List<Long> taggedAccountIds = new ArrayList<>();

    private Long commentLogId;

    private String oldValue;

    private String newValue;

    private Long punchRequestId;

    private Boolean isUpdation;
}
