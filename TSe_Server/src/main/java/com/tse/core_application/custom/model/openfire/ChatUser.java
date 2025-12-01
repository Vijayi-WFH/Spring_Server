package com.tse.core_application.custom.model.openfire;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatUser {

    private String username;

    private String name;

    private String email;

    private String password;

    private List<ChatUserProperty> properties;

}
