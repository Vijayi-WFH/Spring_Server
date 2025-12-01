package com.tse.core_application.model.geo_fencing.attendance;

import com.tse.core_application.constants.geo_fencing.AttendanceStatus;
import com.tse.core_application.model.Constants;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Daily attendance rollup for an account.
 * Phase 6a: Basic entity structure.
 */
@Entity
@Table(name = "attendance_day", schema= Constants.SCHEMA_NAME)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "date_key", nullable = false)
    private LocalDate dateKey;

    @Column(name = "first_in_utc")
    private LocalDateTime firstInUtc;

    @Column(name = "last_out_utc")
    private LocalDateTime lastOutUtc;

    @Column(name = "worked_seconds", nullable = false)
    private Integer workedSeconds = 0;

    @Column(name = "break_seconds", nullable = false)
    private Integer breakSeconds = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Type(type = "jsonb")
    @Column(name = "anomalies", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> anomalies = new HashMap<>();
}
