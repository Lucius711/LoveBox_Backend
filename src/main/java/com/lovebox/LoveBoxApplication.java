package com.lovebox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoveBoxApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoveBoxApplication.class, args);
    }
}
