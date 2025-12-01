package com.example.chat_app.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ActivateDeactivateUserRequest {

    @NonNull
    private List<Long> accountIds;
    @NonNull
    private Boolean isToDeactivate;
}
