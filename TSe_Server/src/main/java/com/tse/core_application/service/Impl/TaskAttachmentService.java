package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.FileMetadata;
import com.tse.core_application.custom.model.SearchCriteria;
import com.tse.core_application.custom.model.SearchOperation;
import com.tse.core_application.dto.DownloadAttachmentResponse;
import com.tse.core_application.dto.ScanResult;
import com.tse.core_application.dto.UploadAttachmentResponse;
import com.tse.core_application.exception.FileNameException;
import com.tse.core_application.exception.FileNotFoundException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.AccessDomain;
import com.tse.core_application.model.RoleAction;
import com.tse.core_application.model.Task;
import com.tse.core_application.model.TaskAttachment;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.ITaskAttachmentService;
import com.tse.core_application.specification.TaskAttachmentSpecification;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.ComponentUtils;
import com.tse.core_application.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.tse.core_application.model.Organization;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.tse.core_application.model.Constants.ActionId.ALL_TASK_BASIC_UPDATE;
import static com.tse.core_application.model.Constants.EntityTypes.TEAM;

@Service
public class TaskAttachmentService implements ITaskAttachmentService {

    private static final Logger logger = LogManager.getLogger(TaskAttachmentService.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private TaskServiceImpl taskServiceImpl;
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    RoleActionRepository roleActionRepository;
    @Autowired
    AccessDomainRepository accessDomainRepository;
    @Autowired
    TaskAttachmentHistoryRepository taskAttachmentHistoryRepository;
    @Autowired
    TaskAttachmentHistoryService taskAttachmentHistoryService;
    @Autowired
    UserAccountService userAccountService;
    @Autowired
    OrganizationRepository organizationRepository;


    /**
     * This is the method which will save the given files for the given taskId. This method can save the single as well as the
     * multiple files for the given task.
     *
     * @param files             The file/files which has to be saved.
     * @param taskId            The taskId for which the given file has to be saved.
     * @param uploaderAccountId The accountId of the uploader.
     * @return HashMap<String, Object>
     * @throws IOException, FileNameException, DuplicateFileException
     */

    @Override
    public HashMap<String, Object> saveFiles(List<MultipartFile> files, Long taskId, Long uploaderAccountId) throws IOException {

        Task task = taskRepository.findById(taskId).orElseThrow(() -> new EntityNotFoundException("Task not found"));
        if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
            throw new ValidationFailedException("User not allowed to add attachments in a deleted Work Item");
        }
        if (task.getFkAccountIdAssigned() == null || !Objects.equals(task.getFkAccountIdAssigned().getAccountId(), uploaderAccountId)) {
            List<RoleAction> roleActionList = roleActionRepository.findByActionId(ALL_TASK_BASIC_UPDATE);
            List<Integer> roleIdsWithAllTaskBasicUpdateAction = roleActionList.stream().map(RoleAction::getRoleId).collect(Collectors.toList());
            List<AccessDomain> accessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(TEAM, task.getFkTeamId().getTeamId(), uploaderAccountId, true);
            List<Integer> roleIdsOfUser = accessDomains.stream().map(AccessDomain::getRoleId).collect(Collectors.toList());
            if (!CommonUtils.containsAny(roleIdsWithAllTaskBasicUpdateAction, roleIdsOfUser)) {
                throw new ValidationFailedException("You're not authorized to upload attachment in this Work Item");
            }
        }


        ArrayList<TaskAttachment> taskAttachmentsToSave = new ArrayList<>();
        ArrayList<UploadAttachmentResponse> taskAttachmentNotSavedDb = new ArrayList<>();
        ArrayList<UploadAttachmentResponse> taskAttachmentDuplicateNotSaved = new ArrayList<>();
        List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(taskId, Constants.FileAttachmentStatus.A);
        ArrayList<String> allTaskAttachmentsNamesFoundWithFileStatusA = new ArrayList<>();

        for(FileMetadata file : allTaskAttachmentsFoundWithFileStatusA) {
            allTaskAttachmentsNamesFoundWithFileStatusA.add(file.getFileName());
        }

        for (MultipartFile file : files) {
            ScanResult scanResult = ComponentUtils.scanFile(file);
            if (!scanResult.getStatus().equals("PASSED")) {
                throw new ValidationFailedException("File scan failed for: " + file.getOriginalFilename() + ".The file might be infected or corrupted.");
            }

            if (file.getOriginalFilename() != null) {
                String filename = StringUtils.cleanPath(file.getOriginalFilename());
                if (!FileUtils.isFilenameValidated(filename) || !FileUtils.isFileExtensionValidated(filename)) {
                    taskAttachmentNotSavedDb.add(new UploadAttachmentResponse(filename, (double) file.getSize()));
                } else {
                    if (allTaskAttachmentsNamesFoundWithFileStatusA.contains(filename)) {
                        taskAttachmentDuplicateNotSaved.add(new UploadAttachmentResponse(filename, (double) file.getSize()));
                    } else {
                        TaskAttachment taskAttachment = new TaskAttachment();
                        taskAttachment.setTaskId(taskId);
                        taskAttachment.setFileContent(file.getBytes());
                        taskAttachment.setFileName(filename);
                        taskAttachment.setFileSize((double) file.getSize());
                        taskAttachment.setFileStatus(Constants.FileAttachmentStatus.A);
                        taskAttachment.setFileType(file.getContentType());
                        taskAttachment.setUploaderAccountId(uploaderAccountId);
                        taskAttachmentsToSave.add(taskAttachment);
                    }
                }
            } else {
                throw new FileNameException();
            }
        }

        List<TaskAttachment> taskAttachmentsSavedDb = taskAttachmentRepository.saveAll(taskAttachmentsToSave);
        ArrayList<UploadAttachmentResponse> savedDbAttachmentsResponse = new ArrayList<>();

        Long version = taskAttachmentHistoryRepository.findMaxVersionByTaskId(taskId);
        if (version == null) {
            version = 0L;
        }

        for (TaskAttachment taskAttachment : taskAttachmentsSavedDb) {
            UploadAttachmentResponse attachmentResponse = new UploadAttachmentResponse();
            attachmentResponse.setFileSize(taskAttachment.getFileSize());
            attachmentResponse.setFileFullName(taskAttachment.getFileName());
            savedDbAttachmentsResponse.add(attachmentResponse);

            // Add Attachment history
            taskAttachmentHistoryService.addTaskAttachmentHistory(taskId, taskAttachment.getFileName(), uploaderAccountId, true, version);
        }

        List<FileMetadata> allActiveTaskAttachmentsFound = taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(taskId, Constants.FileAttachmentStatus.A);

        taskServiceImpl.updateTaskAttachmentsByTaskId(objectMapper.writeValueAsString(allActiveTaskAttachmentsFound), taskId);
        HashMap<String, Object> objectHashMap = new HashMap<>();
        objectHashMap.put("success", savedDbAttachmentsResponse);
        objectHashMap.put("fail", taskAttachmentNotSavedDb);
        objectHashMap.put("duplicate", taskAttachmentDuplicateNotSaved);
        return objectHashMap;

    }

    /**
     * This is the method which finds the given attachment for the given task. The given attachment will only be found for the
     * given task, if the file status is "A" for the given task.
     *
     * @param taskId     The taskId of the task for which the attachment has to be found.
     * @param fileName   The fileName.
     * @param fileStatus The fileStatus.
     * @return The DownloadAttachmentResponse.
     * @throws FileNotFoundException
     */
    @Override
    public DownloadAttachmentResponse getTaskAttachmentByTaskIDAndFileNameAndFileStatus(Long taskId, String fileName, Character fileStatus) {

        DownloadAttachmentResponse downloadAttachmentResponse = null;

        TaskAttachmentSpecification taSpecification = new TaskAttachmentSpecification();
        taSpecification.addSearchCriteria(new SearchCriteria("taskId", taskId, SearchOperation.EQUAL));
        taSpecification.addSearchCriteria(new SearchCriteria("fileName", fileName, SearchOperation.EQUAL));
        taSpecification.addSearchCriteria(new SearchCriteria("fileStatus", fileStatus, SearchOperation.EQUAL));

        Optional<TaskAttachment> taskAttachmentFoundDb = taskAttachmentRepository.findOne(taSpecification);
        if (taskAttachmentFoundDb.isPresent()) {
            downloadAttachmentResponse = new DownloadAttachmentResponse(taskAttachmentFoundDb.get().getFileName(), taskAttachmentFoundDb.get().getFileContent());
        } else {
            throw new FileNotFoundException(fileName);
        }

        return downloadAttachmentResponse;

    }

    /**
     * This is the method which delete the attachment for the given task.
     *
     * @param taskId           The taskId.
     * @param fileName         The fileName.
     * @param optionIndicator  The option indicator.
     * @param removerAccountId The remover accountId.
     * @return String.
     * @throws FileNameException
     */
    @Override
    public String deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(Long taskId, String fileName, String optionIndicator, Long removerAccountId) throws IOException {
        String message = null;
        Double fileSizeUsed=0.0;

        if (optionIndicator != null) {
            Optional<Double> totalSize;
            List<String> allFileNames = new ArrayList<>();
            if (optionIndicator.equalsIgnoreCase(Constants.FileAttachmentOptionIndicator.OPTION_INDICATOR_ALL) && fileName.isEmpty()) {
                List<FileMetadata> allActiveTaskAttachmentsFound = taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(taskId, Constants.FileAttachmentStatus.A);

                allFileNames = allActiveTaskAttachmentsFound.stream()
                        .filter(file -> file.getCommentLogId() == null)
                        .map(FileMetadata::getFileName)
                        .collect(Collectors.toList());

                taskAttachmentRepository.updateAllTaskAttachmentsStatusByTaskIdWithoutComment(taskId, removerAccountId, Constants.FileAttachmentStatus.D);

                totalSize = allActiveTaskAttachmentsFound.stream()
                        .filter(file -> file.getCommentLogId() == null)
                        .map(FileMetadata::getFileSize)
                        .filter(Objects::nonNull)
                        .reduce((previousTotalFileSize, newFileSize) -> previousTotalFileSize + newFileSize);
                fileSizeUsed= totalSize.get();

                List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(taskId, Constants.FileAttachmentStatus.A);

                taskServiceImpl.updateTaskAttachmentsByTaskId(objectMapper.writeValueAsString(allTaskAttachmentsFoundWithFileStatusA), taskId);

                message = "success";
            } else {
                if (optionIndicator.equalsIgnoreCase(Constants.FileAttachmentOptionIndicator.OPTION_INDICATOR_SINGLE) && fileName != null && !fileName.isEmpty()) {
                    allFileNames.add(fileName);
                    List<FileMetadata> activeFilesBeforeDelete = taskAttachmentRepository
                            .findFileMetadataByTaskIdAndFileStatus(taskId, Constants.FileAttachmentStatus.A);

                    for (FileMetadata metadata : activeFilesBeforeDelete) {
                        if (fileName.equals(metadata.getFileName()) && metadata.getFileSize() != null) {
                            if (metadata.getCommentLogId() != null) {
                                throw new ValidationFailedException("Comment's file can't be removed");
                            }
                            fileSizeUsed = metadata.getFileSize();
                        }
                    }

                    taskAttachmentRepository.updateTaskAttachmentStatusByTaskIAndFileName(taskId, fileName, removerAccountId, Constants.FileAttachmentStatus.D);
                    List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(taskId, Constants.FileAttachmentStatus.A);

                    taskServiceImpl.updateTaskAttachmentsByTaskId(objectMapper.writeValueAsString(allTaskAttachmentsFoundWithFileStatusA), taskId);

                    message = "success";
                } else {
                    throw new FileNameException(fileName, optionIndicator);
                }
            }
            Long orgId = userAccountService.getActiveUserAccountByAccountId(removerAccountId).getOrgId();
            Organization organization = organizationRepository.findByOrgId(orgId);
            Long currentQuota = organization.getUsedMemoryQuota() != null ? organization.getUsedMemoryQuota() : 0L;
            Long updatedQuota = currentQuota - fileSizeUsed.longValue();
            organization.setUsedMemoryQuota(updatedQuota);

            if (allFileNames != null && !allFileNames.isEmpty()) {
                Long version = taskAttachmentHistoryRepository.findMaxVersionByTaskId(taskId);
                if (version == null) {
                    version = 0L;
                }
                for (String file : allFileNames) {
                    taskAttachmentHistoryService.addTaskAttachmentHistory(taskId, file, removerAccountId, false, version);
                }
            }
        } else {
            message = "Invalid Option Indicator";
        }

        return message;
    }
}
