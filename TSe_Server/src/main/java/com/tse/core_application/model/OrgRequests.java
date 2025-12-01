package com.tse.core_application.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "org_requests", schema = Constants.SCHEMA_NAME)
public class OrgRequests {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_request_id", nullable = false, unique = true)
    private Long orgRequestId;

    @Column(name = "from_org_id", nullable = false)
    private Long fromOrgId;

    @Column(name = "for_user_id", nullable = false)
    private Long forUserId;

    @Column(name = "is_accepted", nullable = false)
    private Boolean isAccepted=false;

}
