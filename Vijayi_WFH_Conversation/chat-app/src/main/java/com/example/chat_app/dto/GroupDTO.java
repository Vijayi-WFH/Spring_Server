package com.example.chat_app.dto;

import com.example.chat_app.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDTO {
    private Long groupId;
    private String name;
    private String description;
    private String groupIconCode;
    private String groupIconColor;
    private Long orgId;
    private List<Long> users;
    private String type;
    private Long entityId;
    private Long entityTypeId;
    private Boolean isActive;
    private User createdByUser;
    private LocalDateTime createdDate;
}
