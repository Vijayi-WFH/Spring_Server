package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "alert", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "alert_title", nullable = false)
    @Convert(converter = DataEncryptionConverter.class)
    private String alertTitle;

    @Column(name = "alert_reason", nullable = false, length = 1000)
    @Convert(converter = DataEncryptionConverter.class)
    private String alertReason;

    @Column(name = "alert_status")
    private String alertStatus;

    @Column(name = "alert_type")
    private String alertType;

    @Column(name = "associated_task_number", length = 40)
    private String associatedTaskNumber;

    @Column(name = "associated_task_id")
    private Long associatedTaskId;

    @ManyToOne(optional = true)
    @JoinColumn(name = "sender_account_id", referencedColumnName = "account_id", nullable = true )
    private UserAccount fkAccountIdSender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receiver_account_id", referencedColumnName = "account_id")
    private UserAccount fkAccountIdReceiver;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", referencedColumnName = "team_id")
    private Team fkTeamId;

    @ManyToOne
    @JoinColumn(name = "project_id", referencedColumnName = "project_id")
    private Project fkProjectId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id", referencedColumnName = "org_id")
    private Organization fkOrgId;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @Column(name = "isDeleted")
    private Boolean isDeleted = false;

}
