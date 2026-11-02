
package com.healthpilot.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HealthPilotServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            HealthPilotServerApplication.class,
            args
        );
    }
}
