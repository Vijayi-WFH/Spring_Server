package com.tse.core_application.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.tse.core_application.exception.UnauthorizedException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Task;
import com.tse.core_application.repository.ProjectRepository;
import com.tse.core_application.repository.TeamRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommonUtils {
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private ProjectRepository projectRepository;

    private static final Logger logger = LogManager.getLogger(CommonUtils.class.getName());

     public static String getRedisKeyForOtp(String username, String deviceId){
         return username+"_"+deviceId;
     }


    public static PrivateKey getPrivateKey(String filename)
            throws Exception {

        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        PKCS8EncodedKeySpec spec =
                new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public static PublicKey getPublicKey(String filename)
            throws Exception {

        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static String createJIDForTeamName(String teamName, Long teamId){
        String JID = teamName.replaceAll("\\s", "") + "_" + teamId;
        return JID;
    }

    /**
     *
     * @param src
     * @param target
     * @function used to copy non-null fields of the source object to the target object
     */
    public static void copyNonNullProperties(Object src, Object target) {
        BeanWrapper srcWrapper = PropertyAccessorFactory.forBeanPropertyAccess(src);
        BeanWrapper tgtWrapper = PropertyAccessorFactory.forBeanPropertyAccess(target);

        for (PropertyDescriptor pd : srcWrapper.getPropertyDescriptors()) {
            if (pd.getName().equals("class")) {
                continue;
            }

            // If the target object does not have this property, skip
            if (!tgtWrapper.isWritableProperty(pd.getName())) {
                continue;
            }

            Object value = srcWrapper.getPropertyValue(pd.getName());
            if (value != null) {
                tgtWrapper.setPropertyValue(pd.getName(), value);
            }
        }
    }

    /**
     * Checks if any element of list1 is contained in list2.
     *
     * @param list1 the first list
     * @param list2 the second list
     * @return true if any element of list1 is contained in list2, false otherwise
     */
    public static boolean containsAny(List<?> list1, List<?> list2) {
        for (Object obj1 : list1) {
            if (list2.contains(obj1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * this method title case the first letter of all the words in a string separated by spaces
     */
    public static String convertToTitleCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String[] words = name.split("\\s+");
        String result = "";

        for (String word : words) {
            if (!word.isEmpty()) {
                result += word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase() + " ";
            }
        }

        // Remove the trailing space
        return result.trim();
    }

    /** method to get current local date in the given timezone */
    public static LocalDate getLocalDateInGivenTimeZone(String timeZone) {
        ZoneId zoneId = ZoneId.of(timeZone);
        // Get the current instant of time and convert it to the local date in the specified timezone
        return Instant.now().atZone(zoneId).toLocalDate();
    }

    public static boolean validateTimeZoneAndScreenNameInHeader(String timeZone, String screenName) {
        if (!isValidScreenName(screenName)) {
            return false;
        }

        return isValidTimeZone(timeZone);
    }

    private static boolean isValidScreenName(String screenName) {
        if (screenName == null || screenName.isEmpty()) {
            return false;
        }

        // Regex to match only alphabetic screen names without spaces
        Pattern pattern = Pattern.compile("^[a-zA-Z]+$");
        return pattern.matcher(screenName).matches();
    }

    private static boolean isValidTimeZone(String timeZone) {
        if (timeZone == null || timeZone.isEmpty() || timeZone.contains(" ")) {
            return false;
        }

        try {
            ZoneId.of(timeZone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** validates if length of plain text after removing the html tags is valid*/
    public static boolean isValidPlainTextLength(String htmlText, Integer length) {
        Document doc = Jsoup.parse(htmlText);
        String plainText = doc.text();
        return plainText.length() <= length;
    }

    // converts the comma separated accountIds in String format to List of Long
    public static List<Long> convertToLongList(String accountIds) {
        List<Long> result = new ArrayList<>();
        if (accountIds != null && !accountIds.trim().isEmpty()) {
            String[] splitIds = accountIds.split(",");
            for (String accountId : splitIds) {
                try {
                    result.add(Long.parseLong(accountId.trim()));
                } catch (NumberFormatException e) {
                    System.err.println("Cannot convert to Long: " + accountId);
                    throw new NumberFormatException();
                }
            }
        }
        return result;
    }


    /** methods returns a list of field names that have non null values in the given object */
    public static List<String> getNonNullFieldNames(Object obj) {
        List<String> nonNullFields = new ArrayList<>();
        Class<?> objClass = obj.getClass();
        Field[] fields = objClass.getDeclaredFields();

        try {
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    nonNullFields.add(field.getName());
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            logger.error("Unable to execute the method getNonNullFieldNames. Error: ", e);
        }

        return nonNullFields;
    }

    /** truncate string above maxLength and replace the last 3 characters with ellipsis*/
    public static String truncateWithEllipsis(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public static void validateDeveloperAccount(String[] developerAccountIds, List<Long> userAccountIds) {
        List<Long> allowedAccountIds = Arrays.stream(developerAccountIds).map(String::trim).map(Long::parseLong).collect(Collectors.toList());
        if (!CommonUtils.containsAny(allowedAccountIds, userAccountIds)) {
            throw new UnauthorizedException("User not authorised");
        }
    }

    public static Integer calculateTaskEstimateAdjustment(Task task) {
        Integer taskEstimate = task.getTaskEstimate() != null ? task.getTaskEstimate() : 0;
        Double userPerceivedPercentage = task.getUserPerceivedPercentageTaskCompleted() != null ? (100 - task.getUserPerceivedPercentageTaskCompleted()) / 100.0 : 1.0;
        return (int) Math.round(taskEstimate * userPerceivedPercentage);
    }

    public static String convertListToString(List<String> inputList, String delimiter) {
        return String.join(delimiter, inputList);
    }

    /**
     * This method we create a new copy of object mapper and set configurations in object mapper using hibernate5Module to ignore lazy loading objects
     * @param objectMapper this is the autowired object mapper which is used to create a copy
     * @return copy of object mapper from request with new configurations
     */
    public static ObjectMapper configureObjectMapper (ObjectMapper objectMapper) {
        ObjectMapper objectMapperForHistory = objectMapper.copy();
        // Register the Hibernate module to handle lazy-loading
        Hibernate5Module hibernateModule = new Hibernate5Module();
        // Configure to not serialize lazy-loaded collections or proxies
        hibernateModule.configure(Hibernate5Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, false);
        hibernateModule.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, false);

        objectMapperForHistory.registerModule(hibernateModule);
        return objectMapperForHistory;
    }

    public static String getAppId(String filename) throws Exception {
        Path path = Paths.get(filename);

        if (!Files.exists(path)) {
            throw new IllegalStateException("App ID file not found: " + filename);
        }

        return new String(Files.readAllBytes(path)).trim();
    }

    public static String getGithubClientId(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("GitHub Client ID file not found: " + filePath);
        }
        return new String(Files.readAllBytes(path)).trim();
    }

    public static String getGithubClientSecret(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("GitHub Client Secret file not found: " + filePath);
        }
        return new String(Files.readAllBytes(path)).trim();
    }

}
