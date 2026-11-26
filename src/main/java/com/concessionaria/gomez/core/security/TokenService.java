package com.concessionaria.gomez.core.security;

import com.concessionaria.gomez.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** Emite o access token (JWT HS256) de um usuário autenticado. */
@Service
@RequiredArgsConstructor
public class TokenService {

    public static final String CLAIM_PERFIL = "perfil";
    public static final String CLAIM_NOME = "nome";

    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;

    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(properties.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(usuario.getEmail())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_NOME, usuario.getNome())
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                .build();

        String valor = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenEmitido(valor, expiraEm);
    }

    public record TokenEmitido(String valor, Instant expiraEm) {
    }
}
