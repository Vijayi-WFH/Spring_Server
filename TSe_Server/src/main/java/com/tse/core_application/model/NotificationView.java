package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_view", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_view_id")
    private Long notificationViewId;

    @OneToOne
    @JoinColumn(name = "notification_id",referencedColumnName = "notification_id")
    private Notification notificationId;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private UserAccount accountId;

    @Column(name = "is_read")
    private Boolean isRead;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "modified_date_time")
    private LocalDateTime modifiedDateTime;

}
