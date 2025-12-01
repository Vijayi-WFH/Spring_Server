package com.tse.core_application.model;

import com.tse.core_application.dto.RegistrationRequest;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name="user_device",schema= Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Device {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="user_device_id")
	private Long userDeviceId;

	@Column(name="user_id")
	private Long userId;

	@Column(name="device_os")
	private String deviceOs;

	@Column(name="device_os_version")
	private String deviceOsVersion;

	@Column(name="device_make")
	private String deviceMake;

	@Column(name="device_model")
	private String deviceModel;

	@Column(name="device_unique_identifier")
	private String deviceUniqueIdentifier;

	@CreationTimestamp
	@Column(name = "created_date_time", updatable = false, nullable = false)
	private Timestamp createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private Timestamp lastUpdatedDateTime;
	
	public Device getDeviceFromReqgistrationReq(RegistrationRequest req, Long userId) {
		this.setUserId(userId);
		this.setDeviceOs(req.getDeviceOs());
		this.setDeviceOsVersion(req.getDeviceOsVersion());
		this.setDeviceMake(req.getDeviceMake());
		this.setDeviceModel(req.getDeviceModel());
		this.setDeviceUniqueIdentifier(req.getDeviceUniqueIdentifier());
		return this;
	}
}
