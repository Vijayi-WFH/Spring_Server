package com.tse.core_application.model;

import com.tse.core_application.utils.IntegerListConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.tse.core_application.constants.Constants;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

import static com.tse.core_application.model.Constants.DEFAULT_OFFICE_MINUTES;

@Entity
@Table(name = "member_details", schema = com.tse.core_application.model.Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MemberDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_details_id")
    private Long memberDetailsId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_status", nullable = false)
    private Constants.WorkStatus workStatus = com.tse.core_application.constants.Constants.WorkStatus.FULL_TIME;

    @Column(name = "work_minutes", nullable = false)
    private Integer workMinutes = DEFAULT_OFFICE_MINUTES;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_pattern")
    private Constants.WorkPattern workPattern = Constants.WorkPattern.DAILY;

    @Column(name = "work_days")
    @Convert(converter = IntegerListConverter.class)
    private List<Integer> workDays = List.of(1,2,3,4,5);

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    public MemberDetails(Long accountId, Integer entityTypeId, Long entityId, com.tse.core_application.constants.Constants.WorkStatus workStatus, Integer workMinutes) {
        this.accountId = accountId;
        this.entityTypeId = entityTypeId;
        this.entityId = entityId;
        this.workStatus = workStatus;
        this.workMinutes = workMinutes;
    }
}
