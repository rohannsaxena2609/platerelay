package dev.platerelay.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventJsonConfiguration {
    @Bean
    ObjectMapper eventObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
