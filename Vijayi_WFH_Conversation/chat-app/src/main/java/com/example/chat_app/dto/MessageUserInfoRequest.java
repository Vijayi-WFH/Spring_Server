package com.example.chat_app.dto;

import com.example.chat_app.constants.DtoValidationErrors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MessageUserInfoRequest {

    private Long groupId;
    @NotEmpty(message = DtoValidationErrors.MessageUsersDto.NON_EMPTY_SENDER_ID)
    private Long senderId;
    private Long receiverId;
    @NotEmpty(message = DtoValidationErrors.MessageUsersDto.NON_EMPTY_MESSAGE_ID)
    private Long messageId;
}
