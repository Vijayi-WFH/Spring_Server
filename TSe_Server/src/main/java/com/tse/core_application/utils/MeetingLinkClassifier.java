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

    // Custom Jitsi-style path (domain varies, path fixed)
    private static final Pattern JITSI_PATH_PATTERN =
            Pattern.compile("(^|/)vijayi-meet(/|$)", Pattern.CASE_INSENSITIVE);

    private MeetingLinkClassifier() {
        // Private constructor to prevent instantiation
    }

    /**
     * Classifies a meeting URL and returns information about the platform.
     *
     * @param rawUrl         The meeting URL to classify
     * @param isExternalLink Whether the link is marked as external by the user
     * @return MeetingLinkInfo containing url, platform, and label
     */
    public static MeetingLinkInfo classify(String rawUrl, Boolean isExternalLink) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return new MeetingLinkInfo(null, PLATFORM_OTHER, LABEL_OTHER);
        }

        String url = normalizeUrl(rawUrl);

        try {
            URI uri = URI.create(url);
            String path = safe(uri.getPath());
            String query = safe(uri.getQuery());

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

            // Check for Custom Jitsi style: path contains /vijayi-meet and query has meetingName=<name>
            if (JITSI_PATH_PATTERN.matcher(path).find()) {
                String meetingName = extractQueryParam(query, "meetingName");
                if (meetingName != null && !meetingName.isBlank()) {
                    return new MeetingLinkInfo(url, PLATFORM_CUSTOM_JITSI, meetingName);
                }
                // If no meetingName param, still classify as Custom Jitsi but with generic label
                return new MeetingLinkInfo(url, PLATFORM_CUSTOM_JITSI, "Jitsi Meet");
            }

            // Default: Other platform
            return new MeetingLinkInfo(url, PLATFORM_OTHER, LABEL_OTHER);

        } catch (Exception e) {
            // If URL parsing fails, return the raw URL with OTHER platform
            return new MeetingLinkInfo(rawUrl, PLATFORM_OTHER, LABEL_OTHER);
        }
    }

    /**
     * Extracts a query parameter value from a query string.
     *
     * @param query The query string (without leading ?)
     * @param key   The parameter key to extract
     * @return The decoded parameter value, or null if not found
     */
    private static String extractQueryParam(String query, String key) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            String k = idx > -1 ? pair.substring(0, idx) : pair;
            if (k.equalsIgnoreCase(key)) {
                String v = idx > -1 ? pair.substring(idx + 1) : "";
                return URLDecoder.decode(v, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * Normalizes a URL by adding https:// scheme if missing.
     *
     * @param url The URL to normalize
     * @return The normalized URL with scheme
     */
    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        // Add scheme if missing
        if (!trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    /**
     * Returns the string or empty string if null.
     */
    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
