package dev.platerelay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlateRelayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlateRelayApplication.class, args);
    }
}
