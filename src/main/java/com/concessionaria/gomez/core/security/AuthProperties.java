package com.concessionaria.gomez.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuração de autenticação ({@code app.auth.*}).
 *
 * @param jwtSecret            segredo HS256 em Base64 (mínimo 256 bits). Sem valor padrão: a aplicação não sobe sem ele.
 * @param issuer               emissor gravado e exigido no token.
 * @param accessTokenTtl       validade do access token (JWT).
 * @param sessionTtl           limite absoluto da sessão de renovação (não estende na rotação).
 * @param refreshReuseGrace    tolerância para reapresentação de um refresh recém-rotacionado (corridas legítimas).
 * @param cookieName           nome do cookie do refresh token.
 * @param cookiePath           caminho do cookie, como o navegador enxerga a rota ({@code /api/auth} atrás do Nginx/Vite).
 * @param cookieSecure         marca o cookie como {@code Secure}.
 * @param maxTentativas        senhas erradas seguidas antes do bloqueio temporário.
 * @param bloqueio             duração do bloqueio temporário.
 * @param csrfHeaderValue      valor exigido em {@code X-Requested-With} nas rotas que usam o cookie.
 * @param corsAllowedOrigins   origens cruzadas permitidas. Vazio = só a mesma origem (padrão).
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        String issuer,
        Duration accessTokenTtl,
        Duration sessionTtl,
        Duration refreshReuseGrace,
        String cookieName,
        String cookiePath,
        Boolean cookieSecure,
        int maxTentativas,
        Duration bloqueio,
        String csrfHeaderValue,
        List<String> corsAllowedOrigins) {

    public AuthProperties {
        issuer = valorOu(issuer, "concessionaria-gomez");
        accessTokenTtl = accessTokenTtl != null ? accessTokenTtl : Duration.ofMinutes(15);
        sessionTtl = sessionTtl != null ? sessionTtl : Duration.ofHours(10);
        refreshReuseGrace = refreshReuseGrace != null ? refreshReuseGrace : Duration.ofSeconds(10);
        cookieName = valorOu(cookieName, "gomez_refresh");
        cookiePath = valorOu(cookiePath, "/api/auth");
        cookieSecure = cookieSecure == null || cookieSecure;
        maxTentativas = maxTentativas > 0 ? maxTentativas : 5;
        bloqueio = bloqueio != null ? bloqueio : Duration.ofMinutes(15);
        csrfHeaderValue = valorOu(csrfHeaderValue, "gomez-spa");
        corsAllowedOrigins = corsAllowedOrigins != null
                ? corsAllowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList()
                : List.of();
    }

    private static String valorOu(String valor, String padrao) {
        return valor != null && !valor.isBlank() ? valor.trim() : padrao;
    }
}
