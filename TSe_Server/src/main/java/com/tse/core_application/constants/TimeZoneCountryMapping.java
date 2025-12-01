package com.tse.core_application.constants;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class TimeZoneCountryMapping {
    private static Map<String, String> timezoneToCountryMap;

    @PostConstruct
    public void init() {
        timezoneToCountryMap = mapTimezonesToCountries();
    }

    public static boolean isValidTimeZoneForCountry(String userTimeZone, String userCountryCode) {
        String countryCode = timezoneToCountryMap.get(userTimeZone);
        return userCountryCode.equals(countryCode);
    }

    private static Map<String, String> mapTimezonesToCountries() {
        Map<String, String> timezoneToCountry = new HashMap<>();
        String[] locales = Locale.getISOCountries();

        for (String countryCode : locales) {
            for (String id : com.ibm.icu.util.TimeZone.getAvailableIDs(countryCode)) {
                timezoneToCountry.put(id, countryCode);
            }
        }
        return timezoneToCountry;
    }
}

