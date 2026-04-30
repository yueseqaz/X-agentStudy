package com.xagentstudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class XAgentStudyApplication {
    public static void main(String[] args) {
        SpringApplication.run(XAgentStudyApplication.class, args);
    }
}
