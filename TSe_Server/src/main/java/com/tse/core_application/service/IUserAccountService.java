package com.tse.core_application.service;

import com.tse.core_application.dto.conversations.ConversationGroup;
import com.tse.core_application.model.User;
import com.tse.core_application.model.UserAccount;

public interface IUserAccountService {

	UserAccount addUserAccount(UserAccount userAccount, ConversationGroup conversationGroup, User user, String timeZone);

	UserAccount getActiveUserAccountByPrimaryEmailAndOrgId(String primaryEmail, Long orgId);
}
