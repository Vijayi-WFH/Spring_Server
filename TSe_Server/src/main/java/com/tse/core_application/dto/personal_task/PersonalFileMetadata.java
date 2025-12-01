package com.tse.core_application.dto.personal_task;

import lombok.Value;

@Value
public class PersonalFileMetadata {

    String fileName;
    Double fileSize;

    public PersonalFileMetadata(Object fileName, Double fileSize) {
        this.fileName = (String) fileName;
        this.fileSize = fileSize;
    }
}
