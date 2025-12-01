package com.tse.core_application.model;

import com.tse.core_application.utils.IntegerListConverter;
import com.tse.core_application.utils.LongListConverter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_features_access", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFeaturesAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_feature_access_id")
    private Long userFeatureAccessId;

    @Column(name = "user_account_id", nullable = false)
    private Long userAccountId;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "action_ids", columnDefinition = "TEXT")
    private List<Integer> actionIds = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "department_type_id")
    private Integer departmentTypeId;
}
