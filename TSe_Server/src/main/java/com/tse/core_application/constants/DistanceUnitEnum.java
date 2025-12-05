package com.tse.core_application.constants;

import lombok.Getter;

/**
 * Enum representing distance units for geo-fence location labels.
 * Used in OrgPreference to configure how distances are displayed.
 */
@Getter
public enum DistanceUnitEnum {

    KM(1, "Kilometers / Meters"),
    MILES(2, "Miles");

    private final int id;
    private final String displayName;

    DistanceUnitEnum(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Get DistanceUnitEnum from its ID.
     *
     * @param id the ID of the distance unit (1 = KM, 2 = MILES)
     * @return the corresponding DistanceUnitEnum
     * @throws IllegalArgumentException if no matching unit is found
     */
    public static DistanceUnitEnum fromId(int id) {
        for (DistanceUnitEnum unit : values()) {
            if (unit.id == id) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Invalid DistanceUnit id: " + id);
    }

    /**
     * Safely get DistanceUnitEnum from its ID, returning default if not found.
     *
     * @param id the ID of the distance unit (can be null)
     * @return the corresponding DistanceUnitEnum, or KM as default
     */
    public static DistanceUnitEnum fromIdOrDefault(Integer id) {
        if (id == null) {
            return KM;
        }
        for (DistanceUnitEnum unit : values()) {
            if (unit.id == id) {
                return unit;
            }
        }
        return KM;
    }
}
