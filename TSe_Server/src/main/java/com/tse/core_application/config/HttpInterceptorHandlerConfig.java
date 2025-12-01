package com.tse.core_application.config;

import com.tse.core_application.handlers.HttpInterceptorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//@Configuration      // when need to use this interceptor, add the annotation
public class HttpInterceptorHandlerConfig implements WebMvcConfigurer{

    @Autowired
    private HttpInterceptorHandler interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HttpInterceptorHandler());
    }

}
