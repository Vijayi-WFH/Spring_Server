package com.tse.core_application.service;

import com.tse.core_application.dto.RegistrationRequest;
import com.tse.core_application.model.UserRole;
import com.tse.core_application.dto.User;

import java.util.List;

public interface IUserService {

	User findByUsername(String username, String password, String timeZone);

	com.tse.core_application.model.User addUser(com.tse.core_application.model.User user, String timeZone);

	com.tse.core_application.model.User getUser(String primaryEmail);

	List<UserRole> getRolesForUser(User user);

	com.tse.core_application.model.User updateUser(com.tse.core_application.model.User user, RegistrationRequest req);


}
