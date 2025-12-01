package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "dashboard_buttons", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardButtons {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dashboard_button_id", nullable = false)
    private Integer dashboardButtonId;

    @Column(name = "dashboard_button_name", length = 50, nullable = false)
    private String dashboardButtonName;

    @Column(name = "dashboard_button_display_name", length = 50, nullable = false)
    private String dashboardButtonDisplayName;

    @Column(name = "dashboard_button_is_active", nullable = false)
    private Integer dashboardButtonIsActive;

    @Column(name = "dashboard_button_desc", length = 100)
    private String dashboardButtonDesc;

    @Column(name = "created_date_time", nullable = false, updatable = false)
    private Timestamp createdDateTime;

    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;
}
