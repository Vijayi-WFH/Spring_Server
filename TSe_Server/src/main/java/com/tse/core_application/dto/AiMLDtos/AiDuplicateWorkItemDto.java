package com.tse.core_application.dto.AiMLDtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AiDuplicateWorkItemDto {

    private List<AiWorkItemDescResponse> results;
}
