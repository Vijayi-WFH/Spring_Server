package com.tse.core_application.dto.conversations;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationUser implements Serializable {

    private Long userId;
    private String firstName;
    private List<ConversationGroup> groups;
    private Long orgId;
    private Boolean isActive;
    private String email;
    private Long accountId;
    private String lastName;
    private String middleName;
    private Boolean isOrgAdmin;
    private Boolean isAdmin;
    private Boolean isDeleted;
}
