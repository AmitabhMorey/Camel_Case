package com.securevoting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SecureQrVotingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureQrVotingSystemApplication.class, args);
    }
}