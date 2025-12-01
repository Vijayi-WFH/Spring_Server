package com.tse.core_application.utils;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TeamIdentifierGenerator {

//    public static String generateUniqueIdentifier(String teamName, List<String> exclusions) {
//        // Normalize the input
//        String normalized = teamName.replaceAll("[^a-zA-Z0-9]", " ").trim();
//
//        // Extract initials
//        String[] words = normalized.split("\\s+");
//        StringBuilder initials = new StringBuilder();
//        for (String word : words) {
//            if (!word.isEmpty() && Character.isLetterOrDigit(word.charAt(0))) {
//                initials.append(word.charAt(0));
//                if (initials.length() == 1 && !Character.isLetter(initials.charAt(0))) {
//                    initials.setCharAt(0, 'A');  // Ensure the first character is always a letter
//                }
//            }
//        }
//
//        // Handle special single word cases
//        if (words.length == 1 && words[0].length() > 1) {
//            initials = new StringBuilder(words[0].substring(0, Math.min(4, words[0].length())));
//        }
//
//        // Ensure at least two characters
//        while (initials.length() < 2) {
//            initials.append(randomAlphanumeric(1));
//        }
//
//        // Limit the maximum length of identifiers to 7
//        if (initials.length() > 7) {
//            initials = new StringBuilder(initials.substring(0, 7));
//        }
//
//        List<String> upperCaseExclusions = exclusions.stream()
//                .map(String::toUpperCase)
//                .collect(Collectors.toList());
//
//
//        // Check uniqueness and modify if necessary
//        String result = initials.toString().toUpperCase();
//        int attempt = 0;
//        while (upperCaseExclusions.contains(result)) {
//            System.out.println("Conflict detected for initials: " + result + ", attempting modification...");
//            result = modifyIdentifier(initials.toString(), ++attempt);
//            if (result.length() > 7) {
//                result = result.substring(0, 7);
//            }
//            if (attempt > 100) {
//                System.out.println("Breaking after 100 attempts to avoid infinite loop.");
//                break;
//            }
//        }
//
//
//        return result.toUpperCase();
//    }
//
//    private static String modifyIdentifier(String base, int attempt) {
//        if (attempt <= 9) {
//            return base + attempt;  // Append numbers from 1 to 9
//        } else {
//            // Randomize the last character after the basic modifications fail
//            return base.substring(0, base.length() - 1) + randomAlphanumeric(attempt % base.length() + 1);
//        }
//    }
//
//    private static String randomAlphanumeric(int count) {
//        String alphanumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//        StringBuilder result = new StringBuilder();
//        Random rand = new Random();
//        while (count-- > 0) {
//            result.append(alphanumeric.charAt(rand.nextInt(alphanumeric.length())));
//        }
//        return result.toString();
//    }

    private static final Random rand = new Random();

    public static String generateUniqueIdentifier(String teamName, List<String> exclusions) {
        // Normalize the input
        String normalized = teamName.replaceAll("[^a-zA-Z0-9]", " ").trim();

        // Extract initials
        String[] words = normalized.split("\\s+");
        StringBuilder teamCode = new StringBuilder();

        // Extract the first letter of each word
        for (String word : words) {
            if (teamCode.length() < 4) {
                if (!word.isEmpty() && Character.isLetter(word.charAt(0))) {
                    teamCode.append(Character.toUpperCase(word.charAt(0)));
                }
            }
        }

        // Handle single word case
        if (words.length == 1 || teamCode.length() < 2) {
            String firstWord = words[0];
            int neededChars = Math.min(firstWord.length(), 4 - teamCode.length());
            teamCode.append(firstWord.substring(1, neededChars).toUpperCase());
        }

        // Ensure proper length
        if (teamCode.length() < 2) {
            while (teamCode.length() < 2) {
                teamCode.append(randomAlphanumeric(1));
            }
        }

        if (teamCode.length() > 4) {
            teamCode = new StringBuilder(teamCode.substring(0, 4));
        }

        List<String> upperCaseExclusions = exclusions.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        // Check uniqueness and modify if necessary
        String result = teamCode.toString();
        int attempt = 0;
        while (upperCaseExclusions.contains(result) && attempt < 100) {
            result = modifyIdentifier(result, attempt);
            if (result.length() > 4) {
                result = result.substring(0, 4);
            }
            attempt++;
        }

        // Ensure first character is a letter
        if (!Character.isLetter(result.charAt(0))) {
            result = 'A' + result.substring(1);
        }

        return result;
    }

    private static String modifyIdentifier(String base, int attempt) {
        if (attempt < 10) {
            // Attempt to append or replace with a number
            if (base.length() < 4) {
                return base + (attempt + 1);
            } else {
                return base.substring(0, base.length() - 1) + (attempt + 1);
            }
        } else {
            // Randomize last few characters
            return base.substring(0, 1) + randomAlphanumeric(3);
        }
    }

    private static String randomAlphanumeric(int count) {
        String alphanumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();
        while (count-- > 0) {
            result.append(alphanumeric.charAt(rand.nextInt(alphanumeric.length())));
        }
        return result.toString();
    }

}

