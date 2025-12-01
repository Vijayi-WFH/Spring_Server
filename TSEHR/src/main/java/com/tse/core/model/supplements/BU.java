package com.tse.core.model.supplements;

import com.tse.core.configuration.DataEncryptionConverter;
import com.tse.core.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Table(name = "bu", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BU {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bu_id", nullable = false)
    private Long buId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "owner_account_id")
    private Long ownerAccountId;

    @Column(name = "bu_name", length = 255, nullable = false)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(min=3, max=50)
    private String buName;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;
}
