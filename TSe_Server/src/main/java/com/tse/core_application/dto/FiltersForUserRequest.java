package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class FiltersForUserRequest {
    @NotNull
    private Long userId;
    @NotNull
    List<String> filterFields;
 }
