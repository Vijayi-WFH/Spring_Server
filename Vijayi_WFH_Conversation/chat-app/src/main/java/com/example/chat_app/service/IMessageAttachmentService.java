package com.example.chat_app.service;

import com.example.chat_app.dto.DeleteMessageAttachmentRequest;
import com.example.chat_app.dto.DownloadAttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public interface IMessageAttachmentService {

    HashMap<String, Object> saveFiles(List<MultipartFile> files, String messageString, List<Long> accountIds) throws IOException;

    DownloadAttachmentResponse getTaskAttachmentByTaskIDAndFileNameAndFileStatus(Long messageId, String fileName, Long messageAttachmentId, List<Long> accountIds);

    String deleteAttachmentsByAttachmentId(DeleteMessageAttachmentRequest messageAttachmentRequest) throws IOException;

}
