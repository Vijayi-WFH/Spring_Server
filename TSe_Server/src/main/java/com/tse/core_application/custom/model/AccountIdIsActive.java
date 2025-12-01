package com.tse.core_application.custom.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Value
public class AccountIdIsActive {
    Long accountId;
    Boolean isActive;
}
