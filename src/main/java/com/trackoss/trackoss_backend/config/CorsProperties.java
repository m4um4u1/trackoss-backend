
package com.trackoss.trackoss_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    // Getter and setter for allowedOrigins
    private List<String> allowedOrigins = List.of("http://localhost:4200");

}
