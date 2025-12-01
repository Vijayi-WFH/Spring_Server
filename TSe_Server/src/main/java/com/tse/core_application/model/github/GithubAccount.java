package com.tse.core_application.model.github;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "github_account", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GithubAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "github_account_id", nullable = false)
    private Long userGithubAccountId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User fkUserId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "github_user_code", nullable = false)
    private String githubUserCode;

    @Column(name = "github_access_token", nullable = false, length = 1000)
    @Convert(converter = DataEncryptionConverter.class)
    private String githubAccessToken;

    @Column(name = "github_user_name", nullable = false)
    private String githubUserName;

    @Column(name = "is_linked", nullable = false)
    private Boolean isLinked = true;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;
}
