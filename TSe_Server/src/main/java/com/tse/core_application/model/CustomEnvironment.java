package com.tse.core_application.model;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "custom_environment", schema = Constants.SCHEMA_NAME)
public class CustomEnvironment{
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "custom_environment_id", nullable = false, unique = true)
        private Integer customEnvironmentId;

        @NotNull(message = "environmentDisplayName cannot be null")
        @Size(min = 2, max = 70, message = "environmentDisplayName must be between 2 and 70 characters")
        @Column(name = "environment_display_name", nullable = false, length = 70)
        private String environmentDisplayName;

        @Column(name = "environment_description", nullable = false, length = 255)
        private String environmentDescription;

        @Column(name="entity_type_id", nullable = false)
        private Integer entityTypeId;

        @Column(name="entity_id",nullable =false)
        private Long entityId;

        @CreationTimestamp
        @Column(name = "created_date_time", updatable = false, nullable = false)
        private Timestamp createdDateTime;

        @UpdateTimestamp
        @Column(name = "updated_date_time", insertable = false)
        private Timestamp UpdatedDateTime;

        @Column(name="is_active",nullable = false)
        private Boolean isActive;
}
