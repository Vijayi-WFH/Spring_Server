package com.tse.core_application.dto.conversations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupUpdateRequest implements Serializable {

    private String groupName;
    private String groupDesc;
    private Boolean isActive;
}
