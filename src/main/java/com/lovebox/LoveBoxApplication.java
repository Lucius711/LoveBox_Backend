package com.lovebox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LoveBoxApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoveBoxApplication.class, args);
    }
}
