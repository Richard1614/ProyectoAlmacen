package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.demo.almacen")
public class AustralPilotApplication {
    public static void main(String[] args) {
        SpringApplication.run(AustralPilotApplication.class, args);
    }
}
