package com.pulsepay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulsePayApplication {
    public static void main(String[] args) { SpringApplication.run(PulsePayApplication.class, args); }
}

