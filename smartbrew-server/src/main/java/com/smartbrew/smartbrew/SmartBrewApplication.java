package com.smartbrew.smartbrew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartBrewApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartBrewApplication.class, args);
    }
}
