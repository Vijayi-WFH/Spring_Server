package com.tse.core_application.dto.leave;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCertificateMetaData {

    private byte[] doctorCertificate;
    private String fileName;
    private String fileType;
    private Long fileSize;
}
