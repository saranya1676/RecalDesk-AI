package com.recalldesk.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * RecallDesk AI — Memory-Powered Customer Support Intelligence Agent
 * HackWithHyderabad 3.0
 *
 * "Support that remembers."
 */
@SpringBootApplication
@EnableAsync
public class RecallDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecallDeskApplication.class, args);
    }
}
