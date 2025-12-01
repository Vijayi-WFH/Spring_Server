package com.tse.core_application.dto.leave.Response;

import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import lombok.Data;

import java.util.List;

@Data
public class UserListForLeave {
    private List<EmailFirstLastAccountId> approverList;
    private List<EmailFirstLastAccountId> notifiyToList;
}
