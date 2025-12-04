package com.tse.core_application.utils;

import com.tse.core_application.dto.meeting.MeetingLinkInfo;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Utility class to classify meeting links and determine the platform.
 * Supports Google Meet, Zoom, Microsoft Teams, Webex, Custom Jitsi, and Other platforms.
 */
public class MeetingLinkClassifier {

    // Platform constants
    public static final String PLATFORM_GOOGLE_MEET = "GOOGLE_MEET";
    public static final String PLATFORM_ZOOM = "ZOOM";
    public static final String PLATFORM_MICROSOFT_TEAMS = "MICROSOFT_TEAMS";
    public static final String PLATFORM_WEBEX = "WEBEX";
    public static final String PLATFORM_CUSTOM_JITSI = "CUSTOM_JITSI";
    public static final String PLATFORM_OTHER = "OTHER";

    // Label constants
    public static final String LABEL_GOOGLE_MEET = "Google Meet";
    public static final String LABEL_ZOOM = "Zoom";
    public static final String LABEL_MICROSOFT_TEAMS = "Microsoft Teams";
    public static final String LABEL_WEBEX = "Webex";
    public static final String LABEL_OTHER = "Other link";

    // Regex patterns for platform detection
    private static final Pattern GOOGLE_MEET_PATTERN =
            Pattern.compile("^https?://(meet\\.google\\.com|hangouts\\.google\\.com)(/|$)", Pattern.CASE_INSENSITIVE);

    private static final Pattern ZOOM_PATTERN =
            Pattern.compile("^https?://([\\w-]+\\.)?zoom\\.us/(j|my)/", Pattern.CASE_INSENSITIVE);

    private static final Pattern MICROSOFT_TEAMS_PATTERN =
            Pattern.compile("^https?://(teams\\.microsoft\\.com|teams\\.live\\.com)(/|$)", Pattern.CASE_INSENSITIVE);

    private static final Pattern WEBEX_PATTERN =
            Pattern.compile("^https?://([\\w-]+\\.)?webex\\.com(/|$)", Pattern.CASE_INSENSITIVE);

    private MeetingLinkClassifier() {
        // Private constructor to prevent instantiation
    }

    /**
     * Classifies a meeting URL and returns information about the platform.
     *
     * If isExternalLink is null or false, it's treated as an internal Jitsi room name.
     * The value is returned as-is (trimmed) for frontend to handle.
     *
     * If isExternalLink is true, it's treated as an external URL.
     * The URL is normalized (https:// added if missing) and platform is detected.
     *
     * @param rawValue       The meeting link or room name
     * @param isExternalLink Whether the link is marked as external by the user
     * @return MeetingLinkInfo containing url, platform, and label
     */
    public static MeetingLinkInfo classify(String rawValue, Boolean isExternalLink) {
        if (rawValue == null || rawValue.isBlank()) {
            return new MeetingLinkInfo(null, null, null);
        }

        String trimmedValue = rawValue.trim();

        // If isExternalLink is null or false, treat as internal Jitsi room name
        if (isExternalLink == null || !isExternalLink) {
            // Internal link - just return the trimmed room name
            // Frontend will handle how to open it in Jitsi
            return new MeetingLinkInfo(trimmedValue, PLATFORM_CUSTOM_JITSI, trimmedValue);
        }

        // External link - normalize URL and detect platform
        String url = normalizeExternalUrl(trimmedValue);

        try {
            // Check for Google Meet
            if (GOOGLE_MEET_PATTERN.matcher(url).find()) {
                return new MeetingLinkInfo(url, PLATFORM_GOOGLE_MEET, LABEL_GOOGLE_MEET);
            }

            // Check for Zoom
            if (ZOOM_PATTERN.matcher(url).find()) {
                return new MeetingLinkInfo(url, PLATFORM_ZOOM, LABEL_ZOOM);
            }

            // Check for Microsoft Teams
            if (MICROSOFT_TEAMS_PATTERN.matcher(url).find()) {
                return new MeetingLinkInfo(url, PLATFORM_MICROSOFT_TEAMS, LABEL_MICROSOFT_TEAMS);
            }

            // Check for Webex
            if (WEBEX_PATTERN.matcher(url).find()) {
                return new MeetingLinkInfo(url, PLATFORM_WEBEX, LABEL_WEBEX);
            }

            // Default: Other external platform
            return new MeetingLinkInfo(url, PLATFORM_OTHER, LABEL_OTHER);

        } catch (Exception e) {
            // If URL parsing fails, return the trimmed value with OTHER platform
            return new MeetingLinkInfo(trimmedValue, PLATFORM_OTHER, LABEL_OTHER);
        }
    }

    /**
     * Normalizes an external URL by adding https:// scheme if missing.
     *
     * @param url The URL to normalize
     * @return The normalized URL with scheme
     */
    private static String normalizeExternalUrl(String url) {
        // Add scheme if missing for external URLs
        if (!url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            return "https://" + url;
        }
        return url;
    }
}
