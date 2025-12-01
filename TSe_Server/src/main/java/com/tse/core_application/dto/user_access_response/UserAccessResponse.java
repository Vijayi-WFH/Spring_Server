package com.tse.core_application.dto.user_access_response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAccessResponse {

    private List<UserOrgAccessStructureResponse> organizations;
}
