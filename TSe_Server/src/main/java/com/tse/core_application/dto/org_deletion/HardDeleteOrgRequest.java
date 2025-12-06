package com.tse.core_application.dto.org_deletion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HardDeleteOrgRequest {

    private Long orgId;

    private Boolean forceDelete = false;
}
