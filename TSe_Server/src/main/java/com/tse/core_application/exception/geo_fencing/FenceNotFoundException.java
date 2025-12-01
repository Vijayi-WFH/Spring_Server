package com.tse.core_application.exception.geo_fencing;

import org.springframework.http.HttpStatus;

public class FenceNotFoundException extends RuntimeException {

    public FenceNotFoundException(Long fenceId, Long orgId) {
        super(
            String.format("Fence with id=%d not found for organization with id=%d", fenceId, orgId)
        );
    }
}
