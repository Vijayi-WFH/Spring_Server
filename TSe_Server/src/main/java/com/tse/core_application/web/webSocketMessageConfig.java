package com.tse.core_application.web;

import com.tse.core_application.config.AppConfigProperties;
import com.tse.core_application.model.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class webSocketMessageConfig implements WebSocketMessageBrokerConfigurer {

	@Autowired
	private AppConfigProperties appConfigProperties;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(Constants.WebSocket.STOMP_END_POINTS)
				.setAllowedOrigins(appConfigProperties.getConfigValue("origin"))
				.withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.setApplicationDestinationPrefixes(Constants.WebSocket.APPLICATION_DESTINATION_PREFIXES);
		registry.enableSimpleBroker(Constants.WebSocket.SIMPLE_BROKER1, Constants.WebSocket.SIMPLE_BROKER2);
		registry.setUserDestinationPrefix(Constants.WebSocket.USER_DESTINATION_PREFIX);
		
	}

}
