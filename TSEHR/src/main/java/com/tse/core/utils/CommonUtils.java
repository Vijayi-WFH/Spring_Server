package com.tse.core.utils;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

public class CommonUtils {
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
}
