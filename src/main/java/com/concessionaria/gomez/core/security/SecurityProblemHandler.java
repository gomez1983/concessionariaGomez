package com.concessionaria.gomez.core.security;

import com.concessionaria.gomez.api.exceptionhandler.Problem;
import com.concessionaria.gomez.api.exceptionhandler.ProblemType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Responde 401 e 403 gerados pelo filtro de segurança no mesmo formato Problem do
 * {@code ApiExceptionHandler}, para o frontend exibir {@code userMessage} como nos demais erros.
 */
@Component
@RequiredArgsConstructor
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String MSG_NAO_AUTENTICADO = "Entre com seu usuário para acessar o painel.";
    static final String MSG_ACESSO_NEGADO = "Seu perfil não tem permissão para esta operação.";

    private final ObjectMapper objectMapper;
    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Mantém o cabeçalho WWW-Authenticate: Bearer (RFC 6750) e depois escreve o corpo Problem
        bearerEntryPoint.commence(request, response, authException);
        escrever(response, HttpStatus.UNAUTHORIZED, ProblemType.NAO_AUTENTICADO, MSG_NAO_AUTENTICADO);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escrever(response, HttpStatus.FORBIDDEN, ProblemType.ACESSO_NEGADO, MSG_ACESSO_NEGADO);
    }

    private void escrever(HttpServletResponse response, HttpStatus status, ProblemType tipo, String mensagem)
            throws IOException {
        Problem problem = Problem.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .type(tipo.getUri())
                .title(tipo.getTitle())
                .detail(mensagem)
                .userMessage(mensagem)
                .build();

        response.setStatus(status.value());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
