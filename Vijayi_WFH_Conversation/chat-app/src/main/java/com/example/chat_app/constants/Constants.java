package com.example.chat_app.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Constants {

    public static class FormattedResponse {
        public static final String SUCCESS = "success";
        public static final String NOTFOUND = "not found";
        public static final String SERVER_ERROR = "server error!";

        public static final String NO_CONTENT = "NO_CONTENT";
        public static final String VALIDATION_ERROR = "Validation Error!";
        public static final String  FORBIDDEN = "Forbidden";
        public static final String WARNING = "WARNING";
        public static final String BAD_REQUEST = "BAD_REQUEST";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
    }
    public static class GroupTypes {
        public static final String ORG_DEFAULT = "SYSTEM_ORG";
        public static final String PROJ_DEFAULT = "SYSTEM_PROJ";
        public static final String TEAM_DEFAULT = "SYSTEM_TEAM";
        public static final String CUSTOM = "CUSTOM";
    }
    public static class ChatTypes {
        public static final Long USER = 1L;
        public static final Long CUSTOM = 2L;
        public static final Long SYSTEM_ORG = 3L;
        public static final Long SYSTEM_BU = 4L;
        public static final Long SYSTEM_PROJ = 5L;
        public static final Long SYSTEM_TEAM = 6L;

        public static Long valueOf(String type) {
            switch (type) {
                case "USER":
                    return USER;
                case "SYSTEM_ORG":
                    return SYSTEM_ORG;
                case "SYSTEM_BU":
                    return SYSTEM_BU;
                case "SYSTEM_PROJ":
                    return SYSTEM_PROJ;
                case "SYSTEM_TEAM":
                    return SYSTEM_TEAM;
                default:
                    return CUSTOM;
            }
        }
    }
    public static class PinAndFavouriteChatTypes {
        public static final Long USER = 1L;
        public static final Long GROUP = 2L;
    }

    public static class FileAttachmentStatus {
        public static final Character A = 'A';
        public static final Character D = 'D';
    }

    public static class FileAttachmentOptionIndicator {
        public static final String OPTION_INDICATOR_ALL = "All";
        public static final String OPTION_INDICATOR_SINGLE = "Single";
    }

    public static final int FILE_NAME_MAX_LENGTH = 100;

    @Getter
    public enum GroupIconEnum{

        DEFAULT( 1, "DEFAULT"),
        DESIGN_TEAM(2, "DESIGN_TEAM"),
        ENGINEERING_TEAM(3, "ENGINEERING_TEAM"),
        OPERATIONAL_TEAM(4, "OPERATIONAL_TEAM"),
        PRODUCT_TEAM(5, "PRODUCT_TEAM"),
        MARKETING_TEAM(6, "MARKETING_TEAM"),
        SALES_TEAM(7, "SALES_TEAM"),
        CUSTOMER_SUPPORT_TEAM(8, "CUSTOMER_SUPPORT_TEAM"),
        FINANCE_TEAM(9, "FINANCE_TEAM"),
        IT_SUPPORT_TEAM(10, "IT_SUPPORT_TEAM"),
        DATA_AND_ANALYTICS_TEAM(11, "DATA_AND_ANALYTICS_TEAM"),
        BUSINESS_DEVELOPMENT_TEAM(12, "BUSINESS_DEVELOPMENT_TEAM"),
        RISK_MANAGEMENT_TEAM(13, "RISK_MANAGEMENT_TEAM"),
        LEGAL_TEAM(14, "LEGAL_TEAM"),
        QA_TEAM(15, "QUALITY_ASSURANCE_TEAM"),
        HUMAN_RESOURCES_TEAM(16, "HUMAN_RESOURCES_TEAM");

         private final int iconId;
         private final String iconName;

         GroupIconEnum(int iconId, String iconName){
            this.iconName = iconName;
            this.iconId = iconId;
         }

        public static GroupIconEnum getIconEnum(String value) {
            if(value!=null) {
                for (GroupIconEnum icon : GroupIconEnum.values()) {
                    if (icon.getIconName().equalsIgnoreCase(value)) {
                        return icon;
                    }
                }
                throw new IllegalArgumentException("Group Icon name is Invalid");
            }
            return null;
        }

        public static boolean isIconPresent(String value){
            if(value!=null) {
                for (GroupIconEnum icon : GroupIconEnum.values()) {
                    if (icon.getIconName().equalsIgnoreCase(value)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    @Getter
    public enum GroupColorEnum{
        DARK_GRAY("#1D1D1F", "DARK_GRAY"),
        ORANGE("#F69145", "ORANGE"),
        BLUE("#275CB2", "BLUE"),
        GREEN("#56BA6F", "GREEN"),
        RED("#D4494C", "RED"),
        PURPLE("#9A56DE", "PURPLE");

        private final String hexCode;
        private final String    value;

        GroupColorEnum(String hexCode, String value) {
            this.hexCode = hexCode;
            this.value = value;
        }

        public static String getColorCode(String value) {
            if (value != null) {
                for (GroupColorEnum icon : GroupColorEnum.values()) {
                    if (icon.getValue().equalsIgnoreCase(value)) {
                        return icon.getHexCode();
                    }
                }
                throw new IllegalArgumentException("Group Icon name is Invalid");
            }
            return null;
        }

        public static String getColorValue(String hexCode) {
            if (hexCode != null) {
                for (GroupColorEnum icon : GroupColorEnum.values()) {
                    if (icon.getHexCode().equalsIgnoreCase(hexCode)) {
                        return icon.getValue();
                    }
                }
                throw new IllegalArgumentException("Group Icon code is Invalid");
            }
            return null;
        }

        public static boolean isColorPresent(String value){
            if(value!=null) {
                for (GroupColorEnum icon : GroupColorEnum.values()) {
                    if (icon.getHexCode().equalsIgnoreCase(value)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    public static class MessageStatusType {
        public static final String NEW = "NEW";
        public static final String GROUP = "GROUP";
        public static final String EDIT = "EDIT";
        public static final String DELETE = "DELETE";
        public static final String TAG = "TAG";
        public static final String REACT = "REACT";
        public static final String DELIVERY_ACK = "DELIVERY_ACK";
        public static final String READ_ACK = "READ_ACK";
        public static final String DELIVERED = "DELIVERED";
        public static final String READ = "READ";
        public static final String INDICATOR = "INDICATOR";
        public static final String DIRECT = "DIRECT";
    }

    public static class NotificationType {
        public static final String CHAT_SYSTEM_ALERTS = "CHAT_SYSTEM_ALERTS";
        public static final String CHAT_MESSAGE_ALERTS = "CHAT_MESSAGE_ALERTS";
    }

    public static class TseServerAPI{
        public static final String NOTIFICATION_API = "/api/firebase-token/conversation-notification";
        public static final String TSE_SCREEN_NAME = "TSE_Server";

    }

    public static class ChatEntityTypes {
        public static final String USER = "1";
        public static final String GROUP = "2";
        public static final String CONVERSATION_CATEGORY_ID = "5";
    }

    public static class NotificationMessageContants {
        public static final String DM_TITLE = "New Message from " ;
        public static final String GROUP_TITLE = "New Message in ";
        public static final String ATTACHMENT_CONTENT = "Attachment ";
        public static final String ADD_USER_CONTENT = " added you to the group: ";
        public static final String REMOVE_USER_CONTENT = " removed you from the group: ";
        public static final String SET_ADMIN_CONTENT = " made you a group admin in ";
        public static final String REMOVE_ADMIN_CONTENT = " has updated your role. You're no longer an admin.";
        public static final String DELETED_MESSAGE = "This message was deleted!";
    }

    public static class SystemAlertType {
        public static final String ADD = "ADD";
        public static final String REMOVE = "REMOVE";
        public static final String SET_ADMIN = "SET_ADMIN";
        public static final String REMOVE_ADMIN = "REMOVE_ADMIN";
    }

    @Getter
    public enum IndicatorStatus{
        OFFLINE(0, "OFFLINE"),
        AVAILABLE(1, "AVAILABLE"),
        IN_MEETING(2, "IN_MEETING"),
        BUSY(3, "BUSY"),
        AWAY(4, "AWAY"),
        ON_LUNCH_BREAK(5, "ON_LUNCH_BREAK");

        private final int indicatorId;
        private final String indicatorMessage;

        IndicatorStatus(int indicatorId, String indicatorMessage){
            this.indicatorId = indicatorId;
            this.indicatorMessage = indicatorMessage;
        }

        public static String getIndicatorMessageById(int indicatorId){
            for (IndicatorStatus status : IndicatorStatus.values()){
                if(status.getIndicatorId() == indicatorId){
                    return status.getIndicatorMessage();
                }
            }
            return IndicatorStatus.OFFLINE.getIndicatorMessage();
        }

        public static List<String> getStatusMessageCollection() {
            return Arrays.stream(IndicatorStatus.values()).map(IndicatorStatus::getIndicatorMessage).collect(Collectors.toList());
        }

        public static List<Integer> getStatusIdCollection(){
            return Arrays.stream(IndicatorStatus.values()).map(IndicatorStatus::getIndicatorId).collect(Collectors.toList());
        }

    }

    @Getter
    public enum EventType{
        JOIN("JOIN"),
        LEAVE("LEAVE");

        private final String value;

        EventType(String value) {
            this.value = value;
        }
    }

    public static class MessageStrings {
        public static final String RECENT_INACTIVE_GROUP_MESSAGE = "You no longer a part of it!" ;
        public static final String INACCESSIBLE_GROUP_MESSAGE = "This group is no longer accessible." ;
    }

    public static class ReadReceiptsStatus {
        public static final String DOUBLE_BLUE_TICK = "DOUBLE_BLUE_TICK";
        public static final String DOUBLE_TICK = "DOUBLE_TICK";
        public static final String SINGLE_TICK = "SINGLE_TICK";
    }
}
