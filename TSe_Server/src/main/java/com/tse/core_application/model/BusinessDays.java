package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "business_days", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BusinessDays {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="business_day_id", nullable = false)
    private Long businessDayId;

    @Column(name="curr_date")
    private LocalDate currDate;

    @Column(name="prev_business_day")
    private LocalDate prevBusinessDate;
}
