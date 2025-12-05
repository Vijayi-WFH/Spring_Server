package com.tse.core_application.utils.geo_fencing;

import com.tse.core_application.constants.DistanceUnitEnum;

/**
 * Utility class for distance conversions and formatting.
 * Provides centralized conversion methods for geo-fence distance displays.
 */
public class DistanceConversionUtil {

    private static final double METERS_PER_KILOMETER = 1000.0;
    private static final double METERS_PER_MILE = 1609.344;

    private DistanceConversionUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert meters to kilometers.
     *
     * @param meters distance in meters
     * @return distance in kilometers
     */
    public static double metersToKilometers(double meters) {
        return meters / METERS_PER_KILOMETER;
    }

    /**
     * Convert meters to miles.
     *
     * @param meters distance in meters
     * @return distance in miles
     */
    public static double metersToMiles(double meters) {
        return meters / METERS_PER_MILE;
    }

    /**
     * Convert kilometers to meters.
     *
     * @param kilometers distance in kilometers
     * @return distance in meters
     */
    public static double kilometersToMeters(double kilometers) {
        return kilometers * METERS_PER_KILOMETER;
    }

    /**
     * Convert miles to meters.
     *
     * @param miles distance in miles
     * @return distance in meters
     */
    public static double milesToMeters(double miles) {
        return miles * METERS_PER_MILE;
    }

    /**
     * Format distance based on the configured distance unit.
     *
     * Rules:
     * - KM mode: If distance >= 1 km, show in km (e.g., "1.4 km").
     *            If distance < 1 km, show in meters (e.g., "350 meters").
     * - MILES mode: Always show in miles (e.g., "0.8 miles").
     *
     * @param meters   distance in meters
     * @param unit     the distance unit preference (KM or MILES)
     * @return formatted distance string
     */
    public static String formatDistance(double meters, DistanceUnitEnum unit) {
        if (unit == null) {
            unit = DistanceUnitEnum.KM;
        }

        if (unit == DistanceUnitEnum.MILES) {
            double miles = metersToMiles(meters);
            return String.format("%.1f miles", miles);
        } else {
            // KM mode with meters fallback for < 1 km
            double km = metersToKilometers(meters);
            if (km >= 1.0) {
                return String.format("%.1f km", km);
            } else {
                // Show in meters for distances less than 1 km
                long metersRounded = Math.round(meters);
                return String.format("%d meters", metersRounded);
            }
        }
    }

    /**
     * Format distance with fence/location name for location labels.
     *
     * @param meters    distance in meters
     * @param unit      the distance unit preference (KM or MILES)
     * @param fenceName the name of the fence/location
     * @return formatted location label (e.g., "1.4 km from office-1")
     */
    public static String formatDistanceLabel(double meters, DistanceUnitEnum unit, String fenceName) {
        String formattedDistance = formatDistance(meters, unit);
        return String.format("%s from %s", formattedDistance, fenceName);
    }
}
