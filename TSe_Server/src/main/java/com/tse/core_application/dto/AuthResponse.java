package com.tse.core_application.dto;

import com.tse.core_application.custom.model.CustomAccessDomain;
import com.tse.core_application.custom.model.UserIdFirstLastName;
import com.tse.core_application.custom.model.CustomRoleAction;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AuthResponse {

	private String token;
	private String error;
	private List<CustomAccessDomain> accessDomains;
	private List<CustomRoleAction> roleActions;
	private UserIdFirstLastName user;
	private Boolean isSignUpComplete = false;

}
