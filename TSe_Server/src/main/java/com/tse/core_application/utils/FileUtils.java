package com.tse.core_application.utils;

import com.tse.core_application.model.Organization;
import com.tse.core_application.repository.OrganizationRepository;
import com.tse.core_application.service.Impl.EntityPreferenceService;
import com.tse.core_application.service.Impl.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;
import java.util.List;

@Component
public class FileUtils {

    @Value("#{'${chat.file.extensions}'.split(',')}")
    private List<String> allowedExtensionsGroupConvInstance;

    @Value("#{'${task.file.extensions}'.split(',')}")
    private List<String> allowedExtensionsTaskInstance;

    @Value("${default.file.size}")
    private Long defaultFileSize;

    private static List<String> allowedExtensionTask;

    private static List<String> allowedExtensionChat;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private EntityPreferenceService entityPreferenceService;

    @Autowired
    private OrganizationRepository organizationRepository;

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
        boolean isFileNameValidated = filename != null && !filename.isEmpty() && filename.length() <= com.tse.core_application.model.Constants.FILE_NAME_MAX_LENGTH;
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
        int maxLength = com.tse.core_application.model.Constants.FILE_NAME_MAX_LENGTH;
        if (cleanedFilename.length() > maxLength) {
            cleanedFilename = cleanedFilename.substring(0, maxLength);
        }

        return cleanedFilename;
    }

    /**
     * method to validate fileSize based on the org preference or default allowed size
     */
    public Boolean validateFileSizeForOrg(Long uploaderAccountId, List<MultipartFile> files) {
        Long orgId = userAccountService.getActiveUserAccountByAccountId(uploaderAccountId).getOrgId();
        Long allowedFileSize = entityPreferenceService.getAllowedFileSizeForEntity(com.tse.core_application.model.Constants.EntityTypes.ORG, orgId);
        Long fileSizedUsed = 0L;
        for (MultipartFile file : files) {
            if (file.getSize() > allowedFileSize) {
                return false;
            }
            fileSizedUsed += file.getSize();
        }
        Organization organization = organizationRepository.findByOrgId(orgId);
        organization.setUsedMemoryQuota((organization.getUsedMemoryQuota() != null ? organization.getUsedMemoryQuota() : 0) + fileSizedUsed);
        if (organization.getMaxMemoryQuota() > 0 && organization.getUsedMemoryQuota() > organization.getMaxMemoryQuota()) {
            throw new IllegalStateException("The organization has exceeded its allocated memory quota.");
        }
        organizationRepository.save(organization);
        return true;
    }
}
