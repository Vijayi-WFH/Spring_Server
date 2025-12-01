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
@Table(name = "team", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Size(min = 3, max = 50)
    @Column(name = "team_name", nullable = false, length = 255)
    @Convert(converter = DataEncryptionConverter.class)
    private String teamName;

    @Size(min = 3, max = 255)
    @Column(name = "team_desc", nullable = false, length = 1000)
    @Convert(converter = DataEncryptionConverter.class)
    private String teamDesc;

    @Column(name = "parent_team_id")
    private Long parentTeamId;

    @Column(name = "chat_room_name", length=255)
    @Convert(converter = DataEncryptionConverter.class)
    private String chatRoomName;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", referencedColumnName = "project_id")
    private Project fkProjectId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id", referencedColumnName = "org_id")
    private Organization fkOrgId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_account_id", referencedColumnName = "account_id")
    private UserAccount fkOwnerAccountId;


}
