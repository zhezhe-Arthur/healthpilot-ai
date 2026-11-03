
package com.healthpilot.server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HealthPilotServerApplication {
    // 启动 Spring Boot 应用
    public static void main(String[] args) {
        SpringApplication.run(
            HealthPilotServerApplication.class,
            args
        );
    }
}
