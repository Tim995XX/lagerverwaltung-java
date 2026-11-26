package de.hilpert.lager;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Titel, Beschreibung und Anmeldung (Knopf "Authorize") für die Swagger-UI unter /swagger-ui.html. */
@Configuration
public class OpenApiKonfiguration {

    @Bean
    OpenAPI lagerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Mini-Lagerverwaltung")
                .version("0.1.0")
                .description("Artikel, Lagerplätze, Wareneingang, Kommissionierung, Transportaufträge und Klärplatz"))
                .components(new Components().addSecuritySchemes("basic",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basic"));
    }
}
