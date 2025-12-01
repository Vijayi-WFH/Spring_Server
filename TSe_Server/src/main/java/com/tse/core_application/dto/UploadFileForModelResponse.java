package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UploadFileForModelResponse {
    private Long meetingId;
    private Integer modelId;
    private EmailFirstLastAccountIdIsActive uploaderUserAccountDetails;
    private List<MeetingFileMetadata> meetingFileMetaDataList;
    private List<ModelFetchedDto> modelFetchedDtoList;
    private LocalDateTime uploadedDateTime;
    private Boolean underProcessing;
}
