package com.tse.core_application.dto.org_response;

import lombok.Data;
import java.util.List;

@Data
public class OrgStructureResponse {
    private Long orgId;
    private String orgName;
    private List<BuDetail> bu;

}
