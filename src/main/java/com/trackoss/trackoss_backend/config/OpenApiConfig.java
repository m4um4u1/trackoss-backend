package com.trackoss.trackoss_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI trackossOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TrackOSS API")
                        .description("Open Source Cycling Route Management System")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TrackOSS Team")
                                .url("https://github.com/m4um4u1/trackoss-backend"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server")
                ));
    }
}