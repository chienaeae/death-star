package com.deathstar.vader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The primary entry point for the Vader backend. Powered by Spring Boot WebMVC and Project Loom
 * (Virtual Threads).
 */
@SpringBootApplication
public class VaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(VaderApplication.class, args);
    }
}
