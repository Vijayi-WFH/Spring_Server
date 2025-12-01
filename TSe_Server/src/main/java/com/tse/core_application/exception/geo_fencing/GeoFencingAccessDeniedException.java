package com.tse.core_application.exception.geo_fencing;

import org.springframework.http.HttpStatus;

public class GeoFencingAccessDeniedException extends RuntimeException {

    public GeoFencingAccessDeniedException() {
        super(
            "Geo-fencing feature is not allowed or not active for the organization"
        );
    }

    public GeoFencingAccessDeniedException(String message) {
        super(
            message
        );
    }
}
