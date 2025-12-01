package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class  AttentionRequest {
    @NotNull(message = ErrorConstant.AttentionRequest.USERNAME_ERROR)
    private String userName;
    @NotNull(message = ErrorConstant.AttentionRequest.TEAMLIST_ERROR)
    private List<Long> teamList;
}
