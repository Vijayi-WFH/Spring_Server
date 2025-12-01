package com.tse.core_application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Properties;

@Component
public class DebugConfig {

    private static DebugConfig instance;

    @Value("${app.debug:false}")
    // Default value is false if not specified in properties
    private boolean debug;

    private DebugConfig() {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
            this.debug = Boolean.parseBoolean(properties.getProperty("app.debug", "false"));
        } catch (IOException e) {
            e.printStackTrace();
            this.debug = false; // Default to false if an error occurs
        }
    }

    public static synchronized DebugConfig getInstance() {
        if (instance == null) {
            instance = new DebugConfig();
        }
        return instance;
    }

    public boolean isDebug() {
        return debug;
    }
}
