package com.tse.core_application.utils.geo_fencing;

/**
 * Utility for geographic calculations.
 * Phase 6a: Basic distance calculation.
 */
public class GeoMath {

    private static final double EARTH_RADIUS_M = 6371000.0; // meters

    /**
     * Calculate distance between two points using Haversine formula.
     *
     * @param lat1 Latitude of point 1 (degrees)
     * @param lon1 Longitude of point 1 (degrees)
     * @param lat2 Latitude of point 2 (degrees)
     * @param lon2 Longitude of point 2 (degrees)
     * @return Distance in meters
     */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_M * c;
    }

    /**
     * Check if a point is within a circular fence.
     *
     * @param pointLat   Point latitude
     * @param pointLon   Point longitude
     * @param centerLat  Fence center latitude
     * @param centerLon  Fence center longitude
     * @param radiusM    Fence radius in meters
     * @return true if point is inside the fence
     */
    public static boolean isWithinFence(double pointLat, double pointLon,
                                        double centerLat, double centerLon, double radiusM) {
        double distance = distanceMeters(pointLat, pointLon, centerLat, centerLon);
        return distance <= radiusM;
    }
}
