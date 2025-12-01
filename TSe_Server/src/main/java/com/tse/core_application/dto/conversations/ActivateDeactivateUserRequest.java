package com.tse.core_application.dto.conversations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ActivateDeactivateUserRequest {

    private List<Long> accountIds;
    private Boolean isToDeactivate;
}
