package com.smartlib;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartlibBackendApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                SmartlibBackendApplication.class,
                args
        );
    }
}