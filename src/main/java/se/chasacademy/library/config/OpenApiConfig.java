package se.chasacademy.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration bean.
 * Customizes the metadata shown in Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library API")
                        .description("""
                                RESTful Library Management API built with Spring Boot.
                                
                                **Versioning:**
                                - `/api/v1/books` — Core endpoints (create, list, get by ID)
                                - `/api/v2/books` — Enriched responses with availability info and version wrapper
                                
                                **Error Handling:**
                                All errors return a standardized JSON body with `timestamp`, `status`, `error`, `message`, and `path`.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ChasAcademy")
                                .url("https://chasacademy.se"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                );
    }
}
