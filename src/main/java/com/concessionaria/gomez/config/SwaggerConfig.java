package com.concessionaria.gomez.config;

import com.concessionaria.gomez.core.security.OperadorAutenticado;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class SwaggerConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";

    static {
        // O operador vem do token, não de parâmetro da requisição: não deve aparecer na documentação
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(OperadorAutenticado.class);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Concessionária Gomez API Rest")
                        .description("API REST moderna para gerenciamento e venda de veículos. "
                                + "Para testar: execute POST /auth/login, copie o accessToken e use o botão Authorize.")
                        .version("0.1.0-BETA")
                        .contact(new Contact()
                                .name("André Gomez")
                                .url("https://github.com/gomez1983/concessionariaGomez")
                                .email("andregc1983@gmail.com"))
                        .license(new License()
                                .name("Apache License Version 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    /** Rotas que não usam o Bearer (login é público; refresh e logout usam o cookie de sessão). */
    private static final Set<String> ROTAS_SEM_BEARER = Set.of("/auth/login", "/auth/refresh", "/auth/logout");

    /** Exige o Bearer em todas as operações, exceto login, refresh e logout. Lista explícita: rota nova exige token. */
    @Bean
    public OpenApiCustomizer exigirBearerForaDaAutenticacao() {
        return openApi -> openApi.getPaths().forEach((caminho, item) -> {
            if (!ROTAS_SEM_BEARER.contains(caminho)) {
                item.readOperations().forEach(operacao ->
                        operacao.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER)));
            }
        });
    }
}
