package ng.edu.unilag.tradeup.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Interactive API docs, served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI tradeUpOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TradeUp API")
                        .version("1.0.0")
                        .description(
                                """
                                A student marketplace for smarter living.

                                Built by Group 15 for COS202 Computer Programming II at the
                                University of Lagos, in support of UN Sustainable Development
                                Goal 12: Responsible Consumption and Production.

                                Browsing is open. Everything else needs a bearer token from
                                POST /api/auth/login.
                                """)
                        .contact(new Contact().name("Group 15, COS202").url("https://unilag.edu.ng"))
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the token returned by /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
