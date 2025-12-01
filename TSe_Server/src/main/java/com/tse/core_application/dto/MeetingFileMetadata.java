package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class MeetingFileMetadata {
    @NotNull(message = ErrorConstant.File.FILE_NAME)
    private String fileName;
    @NotNull(message = ErrorConstant.File.FILE_EXTENSION)
    private String fileExtension;
    @NotNull(message = ErrorConstant.File.FILE_SIZE)
    private Double fileSize;

}
