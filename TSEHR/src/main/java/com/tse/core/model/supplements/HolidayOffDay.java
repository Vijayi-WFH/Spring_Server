package com.tse.core.model.supplements;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tse.core.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "holiday_off_day", schema= Constants.SCHEMA_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HolidayOffDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Long holidayId;

    @Column(name = "date")
    @NotNull
    private LocalDate date;

    @Column(name = "description")
    @NotNull
    @Size(max = 100)
    private String description;

    @Column(name = "is_recurring")
    private boolean isRecurring = false;

    @Column(name = "is_active")
    private boolean isActive = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "entity_preference_id", referencedColumnName = "entity_preference_id")
    @JsonBackReference
    private EntityPreference entityPreference;

    @CreationTimestamp
    @Column(name = "created_date_time")
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time")
    private LocalDateTime lastUpdatedDateTime;
}
