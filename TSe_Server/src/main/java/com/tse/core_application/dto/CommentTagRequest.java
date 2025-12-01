package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class CommentTagRequest {
    @NotNull
    private Long commentLogId;

    @NotNull
    private String[] commentsTags;
}
