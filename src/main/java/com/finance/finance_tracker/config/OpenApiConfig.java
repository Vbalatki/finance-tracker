package com.finance.finance_tracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Tracker API")
                        .version("1.0")
                        .description("""
                                REST API финансового трекера.
                                 
                                Важно: большая часть приложения — это server-rendered
                                Thymeleaf-контроллеры (страницы счетов, транзакций,
                                бюджетов и т.д.), они здесь НЕ документируются, так как
                                возвращают HTML, а не JSON, и не являются частью
                                публичного API. Здесь описан только раздел /api/**
                                (на сегодня — конвертация валют).
                                 
                                Аутентификация в приложении сессионная (Spring Security
                                form login). Чтобы выполнить запросы из Swagger UI
                                ("Try it out"), сначала залогиньтесь на /login в этом
                                же браузере — сессионная cookie будет использована
                                автоматически.
                                """)
                        .contact(new Contact()
                                .name("Finance Tracker Team"))
                        .license(new License()
                                .name("Internal use only")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Локальная разработка")
                ));
    }
}
