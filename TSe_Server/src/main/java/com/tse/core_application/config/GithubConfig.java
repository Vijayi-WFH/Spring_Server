package com.tse.core_application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class GithubConfig {

    @Bean(name = "githubExecutor")
    public Executor githubExecutor() {
        return Executors.newFixedThreadPool(100);
    }
}

