package com.tse.core_application.service.Impl;

import com.tse.core_application.model.Device;
import com.tse.core_application.repository.DeviceRepository;
import com.tse.core_application.service.IDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceService implements IDeviceService {

	@Autowired
	DeviceRepository deviceRepository;
	
	@Override
	public Device addDevice(Device device) {
		return this.deviceRepository.save(device);
	}

	/** gets the information of the latest device by the user Id */
	@Override
	public Device getLatestDeviceInfoByUserId(Long userId) {
		return deviceRepository.findTopByUserIdOrderByCreatedDateTimeDesc(userId);
	}

}
