package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reminder", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id", nullable = false)
    private Long reminderId;

    @Column(name = "reminder_title", length = 1000, nullable = false)
    @Convert(converter = DataEncryptionConverter.class)
    private String reminderTitle;

    @Column(name = "description", length = 5000)
    @Convert(converter = DataEncryptionConverter.class)
    private String description;

    @Column(name = "reminder_date")
    private LocalDate reminderDate;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "reminder_status")
    private String reminderStatus;

    @Column(name = "is_early_reminder_set")
    private Boolean isEarlyReminderSet;

    @Column(name = "early_reminder_time")
    private LocalDateTime earlyReminderTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_account_id", referencedColumnName = "account_id",  updatable = false)
    private UserAccount fkAccountIdCreator;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

}
