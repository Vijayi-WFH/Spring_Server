package com.example.chat_app.service;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.custom.model.SearchCriteria;
import com.example.chat_app.custom.model.SearchOperation;
import com.example.chat_app.dto.*;
import com.example.chat_app.exception.*;
import com.example.chat_app.exception.NullPointerException;
import com.example.chat_app.model.Group;
import com.example.chat_app.model.Message;
import com.example.chat_app.model.MessageAttachment;
import com.example.chat_app.model.User;
import com.example.chat_app.repository.GroupRepository;
import com.example.chat_app.repository.MessageAttachmentRepository;
import com.example.chat_app.repository.MessageRepository;
import com.example.chat_app.repository.UserRepository;
import com.example.chat_app.specification.MessageAttachmentSpecification;
import com.example.chat_app.utils.ComponentUtils;
import com.example.chat_app.utils.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class MessageAttachmentService implements IMessageAttachmentService {

    private static final Logger logger = Logger.getLogger(MessageAttachmentService.class.getName());

    @Autowired
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MessageAttachmentRepository messageAttachmentRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Value("${updateMessageTime}")
    private Long updateMessageTime;
    /**
     * This is the method which will save the given files for the given taskId. This method can save the single as well as the
     * multiple files for the given task.
     *
     * @param files             The file/files which has to be saved.
     * @return HashMap<String, Object>
     * @throws IOException, FileNameException, DuplicateFileException
     */

    @Override
    @Transactional
    public HashMap<String, Object> saveFiles(List<MultipartFile> files, String messageString, List<Long> accountIds) throws IOException {

        MessageDTO messageDTO = objectMapper.readValue(messageString, MessageDTO.class);

        if (accountIds.contains(messageDTO.getSenderId()) && (messageDTO.getReceiverId() != null || messageDTO.getGroupId() != null)) {
            User sender = userRepository.findByAccountIdAndIsActive(messageDTO.getSenderId(), true);
            if (messageDTO.getReceiverId() != null) {
                User receiver = userRepository.findByAccountIdAndIsActive(messageDTO.getReceiverId(), true);
                if (receiver == null || !sender.getOrgId().equals(receiver.getOrgId()))
                    throw new ValidationFailedException("Sender and Receiver are not a part of the same Org.");
            } else {
                Group groupDb = groupRepository.findByGroupId(messageDTO.getGroupId()).orElseThrow(() -> new NullPointerException("Group is not Found for the given groupId"));
                if (groupDb.getGroupUsers().stream().noneMatch(groupUser -> accountIds.contains(groupUser.getUser().getAccountId()) && !groupUser.getIsDeleted()))
                    throw new ValidationFailedException("User is not a part or in-active member of the Group");
            }
        } else
            throw new ValidationFailedException("Invalid senderId, receiverId and/or groupId provided.");


        ArrayList<MessageAttachment> messageAttachmentsToSave = new ArrayList<>();
        ArrayList<UploadAttachmentResponse> taskAttachmentNotSavedDb = new ArrayList<>();

        for (MultipartFile file : files) {
            ScanResult scanResult = ComponentUtils.scanFile(file);
            if (!scanResult.getStatus().equals("PASSED")) {
                throw new ValidationFailedException("File scan failed for: " + file.getOriginalFilename() + ".The file might be infected or corrupted.");
            }

            if (file.getOriginalFilename() != null) {
                String filename = StringUtils.cleanPath(file.getOriginalFilename());
                if (!FileUtils.isFilenameValidated(filename) || !FileUtils.isFileExtensionValidated(filename)) {
                    taskAttachmentNotSavedDb.add(new UploadAttachmentResponse(null, filename, (double) file.getSize()));
                } else {
                    MessageAttachment messageAttachment = new MessageAttachment();
                    messageAttachment.setFileContent(file.getBytes());
                    messageAttachment.setFileName(filename);
                    messageAttachment.setFileSize((double) file.getSize());
                    messageAttachment.setFileStatus(Constants.FileAttachmentStatus.A);
                    messageAttachment.setFileType(file.getContentType());
                    messageAttachmentsToSave.add(messageAttachment);
                }
            } else {
                throw new FileNameException();
            }
        }

        List<MessageAttachment> messageAttachmentsSavedDb = messageAttachmentRepository.saveAll(messageAttachmentsToSave);
        ArrayList<UploadAttachmentResponse> savedDbAttachmentsResponse = getUploadAttachmentResponses(messageAttachmentsSavedDb);

        HashMap<String, Object> objectHashMap = new HashMap<>();
        objectHashMap.put("success", savedDbAttachmentsResponse);
        objectHashMap.put("fail", taskAttachmentNotSavedDb);
        objectHashMap.put("successListIds", savedDbAttachmentsResponse.stream().map(UploadAttachmentResponse::getMessageAttachmentId).collect(Collectors.toList()));
        return objectHashMap;

    }

    private static ArrayList<UploadAttachmentResponse> getUploadAttachmentResponses(List<MessageAttachment> messageAttachmentsSavedDb) {
        ArrayList<UploadAttachmentResponse> savedDbAttachmentsResponse = new ArrayList<>();

        for (MessageAttachment messageAttachment : messageAttachmentsSavedDb) {
            UploadAttachmentResponse attachmentResponse = new UploadAttachmentResponse();
            attachmentResponse.setMessageAttachmentId(messageAttachment.getMessageAttachmentId());
            attachmentResponse.setFileSize(messageAttachment.getFileSize());
            attachmentResponse.setFileFullName(messageAttachment.getFileName());
            savedDbAttachmentsResponse.add(attachmentResponse);
        }
        return savedDbAttachmentsResponse;
    }

    /**
     * This is the method which finds the given attachment for the given task. The given attachment will only be found for the
     * given task, if the file status is "A" for the given task.
     *
     * @param messageId     The taskId of the task for which the attachment has to be found.
     * @param fileName   The fileName.
     * @param messageAttachmentId The fileId.
     * @return The DownloadAttachmentResponse.
     * @throws FileNotFoundException
     */
    @Override
    public DownloadAttachmentResponse getTaskAttachmentByTaskIDAndFileNameAndFileStatus(Long messageId, String fileName, Long messageAttachmentId, List<Long> accountIds) {

        DownloadAttachmentResponse downloadAttachmentResponse = null;

        Message messageDb = messageRepository.findByMessageId(messageId).orElseThrow(() -> new NullPointerException("Message not found for given messageId"));
        Long groupId = messageDb.getGroupId();
        boolean isPartOfGroup = false;
        if(groupId!=null){ //User should be present in group
            Group groupDb = groupRepository.findByGroupId(groupId).orElseThrow(()-> new NullPointerException("Group not found invalid groupId"));
            if(groupDb.getGroupUsers().stream().noneMatch(groupUser -> accountIds.contains(groupUser.getUser().getAccountId()))){
                throw new UnauthorizedActionException("User is not allowed to download the Attachment as not a part of the Group");
            } else isPartOfGroup = true;
        }
        //validations for the direct messages
        if(isPartOfGroup || (messageDb.getReceiverId()!=null && accountIds.contains(messageDb.getReceiverId())) || accountIds.contains(messageDb.getSenderId())){
            MessageAttachmentSpecification taSpecification = new MessageAttachmentSpecification();
            taSpecification.addSearchCriteria(new SearchCriteria("messageId", messageId, SearchOperation.EQUAL));
            taSpecification.addSearchCriteria(new SearchCriteria("messageAttachmentId", messageAttachmentId, SearchOperation.EQUAL));
            taSpecification.addSearchCriteria(new SearchCriteria("fileStatus", 'A', SearchOperation.EQUAL));

            Optional<MessageAttachment> messageAttachmentFoundDb = messageAttachmentRepository.findOne(taSpecification);
            if (messageAttachmentFoundDb.isPresent()) {
                downloadAttachmentResponse = new DownloadAttachmentResponse(messageAttachmentFoundDb.get().getFileName(), messageAttachmentFoundDb.get().getFileContent());
            } else {
                throw new FileNotFoundException(fileName);
            }
        } else {
            throw new UnauthorizedActionException("User is not allowed to download the Attachment");
        }
        return downloadAttachmentResponse;

    }

    /**
     * This is the method which delete the attachment for the given task.
     *
     * @param messageAttachmentRequest   DeleteAttachmentRequest Object
     * @return String.
     * @throws FileNameException
     */
    @Override
    @Transactional
    public String deleteAttachmentsByAttachmentId(DeleteMessageAttachmentRequest messageAttachmentRequest) throws IOException {
        String message = null;

        Long messageId = messageAttachmentRequest.getMessageId();
        Long messageAttachmentId = messageAttachmentRequest.getMessageAttachmentId();
        String optionIndicator = messageAttachmentRequest.getOptionIndicator();
        Long removerAccountId = messageAttachmentRequest.getRemoverAccountId();

        if (optionIndicator != null && removerAccountId!=null) {
            Message messageDb = messageRepository.findByMessageId(messageId).orElseThrow(() -> new NullPointerException("Message not found for given messageId"));
            if(!messageDb.getSenderId().equals(removerAccountId) && !messageDb.getTimestamp().plusMinutes(updateMessageTime).isAfter(LocalDateTime.now())){
                throw new UnauthorizedActionException("User is not allowed to delete the Message Containing Attachment");
            }
            if (optionIndicator.equalsIgnoreCase(Constants.FileAttachmentOptionIndicator.OPTION_INDICATOR_ALL) && messageId != null) {
                int result = messageAttachmentRepository.updateAllMessageAttachmentsStatusByTaskId(messageId, Constants.FileAttachmentStatus.D);
                message = result > 0 ? "success" : "fail";
            } else {
                if (optionIndicator.equalsIgnoreCase(Constants.FileAttachmentOptionIndicator.OPTION_INDICATOR_SINGLE) && messageId != null && messageAttachmentId!=null) {
                    int result = messageAttachmentRepository.updateMessageAttachmentStatusByMessageIdAndFileName(messageId, messageAttachmentId, Constants.FileAttachmentStatus.D);
                    message = result > 0 ? "success" : "fail";
                } else {
                    if (messageAttachmentId == null)
                        throw new ValidationFailedException("For Optional Indicator " + optionIndicator + "attachment should be single.");
                    else throw new FileNotFoundException("Attachment not Found to delete.");
                }
            }

        } else {
            message = "Invalid Option Indicator";
        }
        return message;
    }
}
