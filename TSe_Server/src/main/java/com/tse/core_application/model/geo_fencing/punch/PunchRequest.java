package com.tse.core_application.model.geo_fencing.punch;

import com.tse.core_application.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a punch request created by a manager/admin targeting a user, team, project, or org.
 * The request is "pending" while now is within the active window [requestedDateTime, expiresAt).
 */
@Entity
@Table(name = "punch_request", schema= Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PunchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId; // 1=USER, 2=ORG, 4=PROJECT, 5=TEAM

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "requester_account_id", nullable = false)
    private Long requesterAccountId;

    @Column(name = "requested_datetime", nullable = false)
    private LocalDateTime requestedDatetime;

    @Column(name = "respond_within_minutes", nullable = false)
    private Integer respondWithinMinutes;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private State state = State.PENDING;

    @CreationTimestamp
    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @UpdateTimestamp
    @Column(name = "updated_datetime", insertable = false)
    private LocalDateTime updatedDatetime;

    public enum State {
        PENDING,
        FULFILLED,
        EXPIRED,
        CANCELLED
    }
}
