package com.tse.core_application.model;


import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sprint_history", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SprintHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sprint_history_id")
    private Long sprintHistoryId;

    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "old_value", length = 5000)
    @Convert(converter = DataEncryptionConverter.class)
    private String oldValue;

    @Column(name = "new_value", length = 5000)
    @Convert(converter = DataEncryptionConverter.class)
    private String newValue;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id_last_updated", referencedColumnName = "account_id")
    private UserAccount fkAccountIdLastUpdated;

    @Column(name = "version")
    private Long version;

    public SprintHistory(Long sprintId, String fieldName, String oldValue, String newValue, LocalDateTime modifiedDate, UserAccount fkAccountIdLastUpdated, Long version) {
        this.sprintId = sprintId;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.modifiedDate = modifiedDate;
        this.fkAccountIdLastUpdated = fkAccountIdLastUpdated;
        this.version = version;
    }
}
