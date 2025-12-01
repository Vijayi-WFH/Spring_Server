package com.example.chat_app.utils;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.dto.FileMetadata;
import com.example.chat_app.repository.MessageAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileUtils {

    @Value("#{'${chat.file.extensions}'.split(',')}")
    private List<String> allowedExtensionsGroupConvInstance;

    @Value("#{'${task.file.extensions}'.split(',')}")
    private List<String> allowedExtensionsTaskInstance;

    @Value("${default.file.size}")
    private Long defaultFileSize;

    @Autowired
    private MessageAttachmentRepository messageAttachmentRepository;

    private static List<String> allowedExtensionTask;

    private static List<String> allowedExtensionChat;

    @PostConstruct
    public void init() {
        allowedExtensionTask = allowedExtensionsTaskInstance;
        allowedExtensionChat = allowedExtensionsGroupConvInstance;
    }

    /**
     * The method to validate the fileName.
     *
     * @param filename The fileName.
     * @return boolean values.
     */
    public static boolean isFilenameValidated(String filename) {
        // this is including the extension if a word file name is 95 characters, it will be 100 characters including the .docx
        boolean isFileNameValidated = filename != null && !filename.isEmpty() && filename.length() <= Constants.FILE_NAME_MAX_LENGTH;
        return isFileNameValidated;
    }

    /**
     * The method to validate the file extension.
     *
     * @param fileNameWithExtension The fileName.
     * @return boolean values.
     */
    public static boolean isFileExtensionValidated(String fileNameWithExtension) {
        boolean isExtensionValidated = false;
        List<String> allowedFileExtension = allowedExtensionTask;
        for (String extension : allowedFileExtension) {
            if (StringUtils.endsWithIgnoreCase(fileNameWithExtension, extension)) {
                isExtensionValidated = true;
                break;
            }
        }
        return isExtensionValidated;
    }

    public static String sanitizeFilename(String originalFilename) {
        // Remove path information
        String cleanedFilename = Paths.get(originalFilename).getFileName().toString();
        // Remove potentially unsafe characters
        cleanedFilename = cleanedFilename.replaceAll("[^a-zA-Z0-9-_ ,&()@!.]", "_");
        // Remove leading and trailing spaces
        cleanedFilename = cleanedFilename.trim();

        // Whitelist file extensions
        List<String> allowedExtensions = allowedExtensionChat;
        boolean isAllowed = allowedExtensions.stream().anyMatch(cleanedFilename::endsWith);
        if (!isAllowed) {
            throw new IllegalArgumentException("Disallowed file extension");
        }

        // Limit filename length
        int maxLength = Constants.FILE_NAME_MAX_LENGTH;
        if (cleanedFilename.length() > maxLength) {
            cleanedFilename = cleanedFilename.substring(0, maxLength);
        }

        return cleanedFilename;
    }

    /**
     * method to validate fileSize based on the org preference or default allowed size
     */
    public Boolean validateFileSizeForOrg(Long uploaderAccountId, List<MultipartFile> files) {
//        Long orgId = userAccountService.getActiveUserAccountByAccountId(uploaderAccountId).getOrgId();
//        Long allowedFileSize = entityPreferenceService.getAllowedFileSizeForEntity(com.tse.core_application.model.Constants.EntityTypes.ORG, orgId);
        Long allowedFileSize = defaultFileSize;
        Long fileSizedUsed = 0L;
        for (MultipartFile file : files) {
            fileSizedUsed += file.getSize();
            if (fileSizedUsed > allowedFileSize) {
                return false;
            }
        }
//        Organization organization = organizationRepository.findByOrgId(orgId);
//        organization.setUsedMemoryQuota((organization.getUsedMemoryQuota() != null ? organization.getUsedMemoryQuota() : 0) + fileSizedUsed);
//        if (organization.getMaxMemoryQuota() > 0 && organization.getUsedMemoryQuota() > organization.getMaxMemoryQuota()) {
//            throw new IllegalStateException("The organization has exceeded its allocated memory quota.");
//        }
//        organizationRepository.save(organization);
        return true;
    }

    //ToDo: it can be enhance more as getGroupMessage/getDmMessage api calling this method for every message containing attachmentId.
    public List<FileMetadata> addFileMetaData(String attachmentIds){
        if(attachmentIds!=null && !attachmentIds.isEmpty()){  //returning only active attachment metadata.
            List<Long> attachmentIdList = Arrays.stream(attachmentIds.split(",")).map(Long::valueOf).collect(Collectors.toList());
            return messageAttachmentRepository.findMetadataFromAttachmentIdIn(attachmentIdList, 'A').orElse(null);
        }
        return Collections.emptyList();
    }
}
