package com.tse.core_application.model.geo_fencing.attendance;

import com.tse.core_application.constants.geo_fencing.EventAction;
import com.tse.core_application.constants.geo_fencing.EventKind;
import com.tse.core_application.constants.geo_fencing.EventSource;
import com.tse.core_application.constants.geo_fencing.IntegrityVerdict;
import com.tse.core_application.model.Constants;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a punch event (CHECK_IN, CHECK_OUT, BREAK_START, BREAK_END, PUNCHED).
 * Phase 6a: Basic entity structure for foundational infrastructure.
 */
@Entity
@Table(name = "attendance_event", schema= Constants.SCHEMA_NAME)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_kind", nullable = false)
    private EventKind eventKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_source", nullable = false)
    private EventSource eventSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_action", nullable = false)
    private EventAction eventAction;

    @Column(name = "ts_utc", nullable = false)
    private LocalDateTime tsUtc;

    @Column(name = "client_local_ts")
    private LocalDateTime clientLocalTs;

    @Column(name = "client_tz")
    private String clientTz;

    @Column(name = "fence_id")
    private Long fenceId;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "accuracy_m")
    private Double accuracyM;

    @Column(name = "under_range")
    private Boolean underRange;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false)
    private IntegrityVerdict verdict;

    @Column(name = "fail_reason")
    private String failReason;

    @Type(type = "jsonb")
    @Column(name = "flags", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> flags = new HashMap<>();

    @Column(name = "punch_request_id")
    private Long punchRequestId;

    @Column(name = "requester_account_id")
    private Long requesterAccountId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @PrePersist
    protected void onCreate() {
        if (tsUtc == null) {
            tsUtc = LocalDateTime.now();
        }
    }
}
