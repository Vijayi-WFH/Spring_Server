package com.tse.core_application.exception.geo_fencing;

import org.springframework.http.HttpStatus;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(Long orgId) {
        super(
            "No geo-fencing policy found for the organization"
        );
    }
}
