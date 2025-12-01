package com.example.chat_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MessageUserInfoResponse {

    private Map<Long , LocalDateTime> readUsers;
    private Map<Long , LocalDateTime> deliveredUsers;
    private List<Long> noInfoUsers;
}
