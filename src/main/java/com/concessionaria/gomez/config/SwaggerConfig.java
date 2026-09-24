package com.concessionaria.gomez.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Concessionária Gomez API Rest")
                        .description("API REST moderna para gerenciamento e venda de veículos.")
                        .version("0.1.0-BETA")
                        .contact(new Contact()
                                .name("André Gomez")
                                .url("https://github.com/gomez1983/concessionariaGomez")
                                .email("andregc1983@gmail.com"))
                        .license(new License()
                                .name("Apache License Version 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
