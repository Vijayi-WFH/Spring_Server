package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.LocalDateTime;

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

//    @Size(min = 3, max = 50, message = ErrorConstant.Team.TEAM_NAME)
    @Column(name = "team_name", nullable = false, length = 255)
    @Convert(converter = DataEncryptionConverter.class)
    private String teamName;

//    @Size(min = 3, max = 255, message = ErrorConstant.Team.TEAM_DESC)
    @Column(name = "team_desc", nullable = false, length = 4000)
    @Convert(converter = DataEncryptionConverter.class)
    private String teamDesc;

    @Column(name = "parent_team_id")
    private Long parentTeamId;

    @Column(name = "chat_room_name", length = 255)
    @Convert(converter = DataEncryptionConverter.class)
    private String chatRoomName;

    @Column(name = "team_code", length = 10) // need to add condition nullable = false
    @NotNull(message = "Team code can not be null")
    private String teamCode;

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

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "is_disabled")
    private Boolean isDisabled = false;

    @Column(name = "deleted_on")
    private LocalDateTime deletedOn;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "deleted_by_accountId", referencedColumnName = "account_id")
    private UserAccount fkDeletedByAccountId;

//    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
////    @JsonManagedReference
//        @JsonIgnore
//    private List<Label> teamLabels;

//    @Override
//    public String toString() {
//        return "Team{" +
//                "teamId=" + teamId +
//                ", teamName='" + teamName + '\'' +
//                ", teamDesc='" + teamDesc + '\'' +
//                ", parentTeamId=" + parentTeamId +
//                ", chatRoomName='" + chatRoomName + '\'' +
//                ", createdDateTime=" + createdDateTime +
//                ", lastUpdatedDateTime=" + lastUpdatedDateTime +
//                ", fkProjectId=" + (fkProjectId != null ? fkProjectId.toString() : null) +
//                ", fkOrgId=" + (fkOrgId != null ? fkOrgId.toString() : null) +
//                ", fkOwnerAccountId=" + (fkOwnerAccountId != null ? fkOwnerAccountId.toString() : null) +
//                '}';
//    }


}

