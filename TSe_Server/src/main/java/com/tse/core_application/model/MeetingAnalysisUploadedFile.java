package com.tse.core_application.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.dto.MeetingFileMetadata;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "meeting_analysis_uploaded_file", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MeetingAnalysisUploadedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_analysis_uploaded_file_id", nullable = false)
    private Long meetingAnalysisUploadedFileId;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "model_id", nullable = false)
    private Integer modelId;

    @Column(name = "uploader_account_id", nullable = false)
    private Long uploaderAccountId;

    @Column(name = "meeting_file_metadata_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String meetingFileMetaDataList;

    @Column(name = "uploaded_date_time", nullable = false)
    private LocalDateTime uploadedDateTime;

    @Column(name = "under_processing", nullable = false)
    private Boolean underProcessing = true;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "updated_date_time", insertable = false)
    private LocalDateTime updatedDateTime;

    public List<MeetingFileMetadata> getMeetingFileMetaDataList() {
        return convertJsonToList(meetingFileMetaDataList);
    }

    public void setMeetingFileMetaDataList(List<MeetingFileMetadata> meetingFileMetaDataList) {
        this.meetingFileMetaDataList = convertListToJson(meetingFileMetaDataList);
    }

    private List<MeetingFileMetadata> convertJsonToList(String json) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json, new TypeReference<List<MeetingFileMetadata>>() {});
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String convertListToJson(List<MeetingFileMetadata> list) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error while serializing meeting file metadata list to JSON", e);
        }
    }
}
