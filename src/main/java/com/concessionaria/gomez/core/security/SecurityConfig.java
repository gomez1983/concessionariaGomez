package com.concessionaria.gomez.core.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * Autenticação por JWT (Bearer) e matriz de permissões por rota.
 * A matriz espelha a seção 5 de {@code docs/security/design/2026-11-25-etapa3-autenticacao-rbac.md}
 * e é verificada linha a linha pelo {@code SegurancaMatrizIT}.
 * Regras que dependem de dados (proposta própria, custos, conflito de interesse) ficam no domínio.
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String VENDEDOR = "VENDEDOR";
    private static final String GERENTE = "GERENTE";
    private static final String ADMIN = "ADMIN";
    private static final String[] TODOS = {VENDEDOR, GERENTE, ADMIN};
    private static final String[] GESTAO = {GERENTE, ADMIN};

    /** 256 bits: tamanho mínimo da chave para HS256 (RFC 7518, seção 3.2). */
    private static final int TAMANHO_MINIMO_CHAVE_BYTES = 32;

    private final AuthProperties properties;
    private final SecurityProblemHandler problemHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // A API autentica por cabeçalho Authorization, que o navegador não envia sozinho.
                // As rotas que usam cookie (/auth/refresh e /auth/logout) exigem SameSite=Strict e X-Requested-With.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .authorizeHttpRequests(auth -> auth
                        // Infraestrutura
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()

                        // Autenticação
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/auth/senha").authenticated()

                        // Administração de usuários
                        .requestMatchers(HttpMethod.GET, "/usuarios", "/usuarios/*").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/usuarios", "/usuarios/*/senha-temporaria").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/usuarios/*", "/usuarios/*/ativo").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/*/ativo").hasRole(ADMIN)

                        // Carros
                        .requestMatchers(HttpMethod.GET, "/carros", "/carros/*").hasAnyRole(TODOS)
                        .requestMatchers(HttpMethod.POST, "/carros").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.PUT, "/carros/*", "/carros/*/ativo").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.DELETE, "/carros/*/ativo").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.DELETE, "/carros/*").hasRole(ADMIN)

                        // Fotos (foto principal e galeria)
                        .requestMatchers(HttpMethod.GET,
                                "/carros/*/foto", "/carros/*/fotos", "/carros/*/fotos/*/conteudo").hasAnyRole(TODOS)
                        .requestMatchers(HttpMethod.PUT, "/carros/*/foto", "/carros/*/fotos/*").hasAnyRole(TODOS)
                        .requestMatchers(HttpMethod.POST, "/carros/*/fotos").hasAnyRole(TODOS)
                        .requestMatchers(HttpMethod.DELETE, "/carros/*/foto", "/carros/*/fotos/*").hasAnyRole(GESTAO)

                        // Clientes
                        .requestMatchers(HttpMethod.GET, "/clientes", "/clientes/*").hasAnyRole(TODOS)
                        .requestMatchers(HttpMethod.POST, "/clientes").hasAnyRole(TODOS)
                        .requestMatchers(HttpMethod.PUT, "/clientes/*/ativo").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.DELETE, "/clientes/*/ativo").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.PUT, "/clientes/*").hasAnyRole(TODOS)
                        .requestMatchers(HttpMethod.DELETE, "/clientes/*").hasRole(ADMIN)

                        // Dashboard
                        .requestMatchers(HttpMethod.GET, "/dashboard").hasAnyRole(TODOS)

                        // Propostas (rotas fixas antes de /propostas/*)
                        .requestMatchers(HttpMethod.GET, "/propostas/politica-vigente").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.POST, "/propostas/politicas").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.POST, "/propostas/*/aprovacoes").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.GET, "/propostas", "/propostas/*").hasAnyRole(TODOS)
                        .requestMatchers(HttpMethod.POST, "/propostas", "/propostas/simulacoes").hasAnyRole(TODOS)
                        // Para VENDEDOR, o domínio exige que a proposta seja dele
                        .requestMatchers(HttpMethod.POST,
                                "/propostas/*/revisoes", "/propostas/*/reservar", "/propostas/*/converter-venda")
                        .hasAnyRole(TODOS)

                        // Vendas
                        .requestMatchers(HttpMethod.GET, "/vendas", "/vendas/*").hasAnyRole(TODOS)
                        .requestMatchers(HttpMethod.POST, "/vendas/*/cancelamento-auditado").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.POST, "/vendas").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.PUT, "/vendas/*").hasAnyRole(GESTAO)
                        .requestMatchers(HttpMethod.DELETE, "/vendas/*").hasRole(ADMIN)

                        // Rota sem regra explícita falha fechada
                        .anyRequest().denyAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Prefixo {bcrypt}: permite trocar de algoritmo no futuro sem invalidar senhas existentes
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveAssinatura()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveAssinatura())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origens = properties.corsAllowedOrigins();
        if (!origens.isEmpty()) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(origens);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
            config.setAllowCredentials(true);
            source.registerCorsConfiguration("/**", config);
        }
        // Sem origens configuradas: nenhuma origem cruzada é aceita (o painel usa a mesma origem via proxy)
        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenService.CLAIM_PERFIL);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** Fail-fast: sem segredo (ou com segredo curto) a aplicação não sobe. */
    private SecretKey chaveAssinatura() {
        String segredo = properties.jwtSecret();
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado. Defina app.auth.jwt-secret (Base64, mínimo 256 bits) no .env.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(segredo.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET deve estar em Base64.", e);
        }
        if (bytes.length < TAMANHO_MINIMO_CHAVE_BYTES) {
            throw new IllegalStateException(String.format(
                    "JWT_SECRET muito curto: %d bits. O mínimo para HS256 é 256 bits.", bytes.length * 8));
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
