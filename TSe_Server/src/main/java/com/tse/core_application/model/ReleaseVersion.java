package com.tse.core_application.model;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "release_version", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "release_version_id", nullable = false)
    private Long releaseVersionId;

    @NotBlank(message = ErrorConstant.ReleaseVersion.RELEASE_VERSION_NAME)
    @Size(min = 1, max = 100, message = ErrorConstant.ReleaseVersion.RELEASE_VERSION_NAME_SIZE)
    @Column(name = "release_version_name", nullable = false, length = 100)
    private String releaseVersionName;

    @NotNull(message = ErrorConstant.ENTITY_TYPE_ID)
    @Column(name = "entity_type_id")
    private Integer entityTypeId;

    @NotNull(message = ErrorConstant.ENTITY_ID)
    @Column(name = "entity_id")
    private Long entityId;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "updated_date_time", insertable = false)
    private LocalDateTime updatedDateTime;
}

