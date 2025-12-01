package com.tse.core_application.service.Impl;

import com.tse.core_application.model.Attachment;
import com.tse.core_application.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AttachmentService {

    @Autowired
    private AttachmentRepository attachmentRepository;

    /**
     *
     * @param attachmentId
     * @return Attachment object retrieved from dB by attachment Id
     */
    public Attachment getAttachmentById(Long attachmentId) {
        return attachmentRepository.findById(attachmentId).orElse(null);
    }

}
