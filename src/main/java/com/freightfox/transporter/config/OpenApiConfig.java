package com.freightfox.transporter.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transporterOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transporter Assignment on Lanes API")
                        .version("1.0.0")
                        .description("""
                                Assigns transporters to lanes across India.

                                1. **POST /api/v1/transporters/input** stores lanes and transporter quotes \
                                (replaces any previous dataset).
                                2. **POST /api/v1/transporters/assignment** selects at most `maxTransporters` \
                                transporters that together cover every lane, assigns each lane to the cheapest \
                                selected transporter and minimizes the total cost. Ties are broken by using more \
                                distinct transporters, then by the lexicographically smallest set of ids.

                                The solver is an exact branch-and-bound with a time budget; `optimal` in the \
                                response tells whether optimality was proven. Examples are pre-filled with \
                                Test Case 1 from the assignment brief.""")
                        .contact(new Contact().name("Chandan").email("candidate@example.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("README: algorithm, assumptions and deployment")
                        .url("https://github.com/<GITHUB_USER>/transporter-assignment#readme"));
    }
}
