package com.tse.core_application.dto;

import com.tse.core_application.dto.capacity.UserCapacityDetail;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamCapacityResponse {
    private String message;
    private List<UserCapacityDetail> userCapacityDetails;
}
