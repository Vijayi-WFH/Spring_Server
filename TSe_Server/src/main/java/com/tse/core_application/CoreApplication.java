package com.tse.core_application;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableEncryptableProperties
public class CoreApplication {

    public static void main(String[] args) {
        ThreadContext.put("userId", String.valueOf(0));
        ThreadContext.put("accountId", String.valueOf(0));
        SpringApplication.run(CoreApplication.class, args);
    }

}
