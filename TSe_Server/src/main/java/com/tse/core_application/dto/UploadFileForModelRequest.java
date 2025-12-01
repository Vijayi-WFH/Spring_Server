package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UploadFileForModelRequest {
    @NotNull(message = ErrorConstant.Meeting.MEETING_ID)
    private Long meetingId;
    @NotNull(message = ErrorConstant.ORG_ID_ERROR)
    private Long orgId;
    @NotNull(message = ErrorConstant.Meeting.MODEL_ID)
    private Integer modelId;
    @NotNull(message = ErrorConstant.Meeting.MEETING_FILE_METADATA_LIST)
    private List<MeetingFileMetadata> meetingFileMetaDataList;
    @NotNull(message = ErrorConstant.Meeting.UPLOADED_DATE_TIME)
    private LocalDateTime uploadedDateTime;

}
