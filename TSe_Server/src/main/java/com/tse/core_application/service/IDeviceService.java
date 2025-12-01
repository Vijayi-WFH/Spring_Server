package com.tse.core_application.service;

import com.tse.core_application.model.Device;

//import reactor.core.publisher.Mono;

public interface IDeviceService {
	
	Device addDevice(Device device);
	Device getLatestDeviceInfoByUserId(Long userId);


}
