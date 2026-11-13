package com.ruanvascolabs.camnode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CamNodeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamNodeApiApplication.class, args);
    }
}
