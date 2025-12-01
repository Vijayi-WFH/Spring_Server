package com.example.chat_app.service;

import com.example.chat_app.model.MessageAttachment;
import com.example.chat_app.repository.MessageAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileService {

    @Autowired
    private MessageAttachmentRepository messageAttachmentRepository;

    public MessageAttachment saveFile(MultipartFile file) throws IOException {
        MessageAttachment attachment = new MessageAttachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize((double) file.getSize());
        attachment.setFileContent(file.getBytes());
        attachment.setFileStatus('A'); // Assuming 'A' means active or available

        return messageAttachmentRepository.save(attachment);
    }
}
