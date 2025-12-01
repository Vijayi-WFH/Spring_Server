package com.tse.core_application.service;

import com.tse.core_application.dto.conversations.ConversationGroup;
import com.tse.core_application.model.ExceptionalRegistration;
import com.tse.core_application.model.Organization;
import com.tse.core_application.model.User;

//import reactor.core.publisher.Mono;

public interface IOrganizationService {
	
	Organization addOrganization(Organization organization, ExceptionalRegistration exceptionalRegistration, String userEmail, User user);
	
	Organization getOrganizationByOrganizationName(String orgName);

	Integer getOrganizationCountByEmail(String email);

}
