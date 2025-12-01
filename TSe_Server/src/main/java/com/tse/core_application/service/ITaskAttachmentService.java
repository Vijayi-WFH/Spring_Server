package com.tse.core_application.service;

import com.tse.core_application.dto.DownloadAttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public interface ITaskAttachmentService {

    HashMap<String, Object> saveFiles(List<MultipartFile> files, Long taskId, Long uploaderAccountId) throws IOException;

    DownloadAttachmentResponse getTaskAttachmentByTaskIDAndFileNameAndFileStatus(Long taskId, String fileName, Character fileStatus);

    String deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(Long taskId, String fileName, String optionIndicator, Long removerAccountId) throws IOException;

}
