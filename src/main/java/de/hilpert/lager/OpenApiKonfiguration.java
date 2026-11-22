package de.hilpert.lager;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Titel und Beschreibung für die Swagger-UI unter /swagger-ui.html. */
@Configuration
public class OpenApiKonfiguration {

    @Bean
    OpenAPI lagerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Mini-Lagerverwaltung")
                .version("0.1.0")
                .description("Artikel, Lagerplätze, Wareneingang, Kommissionierung, Transportaufträge und Klärplatz"));
    }
}
