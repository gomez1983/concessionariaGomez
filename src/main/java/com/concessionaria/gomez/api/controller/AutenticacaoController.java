package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.model.TokenAcessoModel;
import com.concessionaria.gomez.api.model.UsuarioLogadoModel;
import com.concessionaria.gomez.api.model.input.LoginInput;
import com.concessionaria.gomez.api.model.input.TrocaSenhaInput;
import com.concessionaria.gomez.core.security.AuthProperties;
import com.concessionaria.gomez.core.security.OperadorAutenticado;
import com.concessionaria.gomez.core.security.TokenService;
import com.concessionaria.gomez.domain.exception.AcessoComercialNegadoException;
import com.concessionaria.gomez.domain.model.Usuario;
import com.concessionaria.gomez.domain.service.AutenticacaoService;
import com.concessionaria.gomez.domain.service.AutenticacaoService.ResultadoAutenticacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login, renovação de sessão e logout do painel")
@RequiredArgsConstructor
public class AutenticacaoController {

    static final String CABECALHO_ANTI_CSRF = "X-Requested-With";

    private final AutenticacaoService autenticacaoService;
    private final TokenService tokenService;
    private final AuthProperties properties;

    @PostMapping("/login")
    @Operation(summary = "Autentica com e-mail e senha. Devolve o access token e grava o refresh token em cookie HttpOnly")
    public ResponseEntity<TokenAcessoModel> login(@RequestBody @Valid LoginInput input) {
        ResultadoAutenticacao resultado = autenticacaoService.autenticar(input.getEmail(), input.getSenha());
        return responder(resultado);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o access token usando o cookie de sessão (exige X-Requested-With)")
    public ResponseEntity<TokenAcessoModel> refresh(
            @CookieValue(name = "${app.auth.cookie-name:gomez_refresh}", required = false) String refreshToken,
            @RequestHeader(value = CABECALHO_ANTI_CSRF, required = false) String antiCsrf) {

        exigirCabecalhoAntiCsrf(antiCsrf);
        return responder(autenticacaoService.renovar(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerra a sessão de renovação e apaga o cookie (exige X-Requested-With)")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.auth.cookie-name:gomez_refresh}", required = false) String refreshToken,
            @RequestHeader(value = CABECALHO_ANTI_CSRF, required = false) String antiCsrf) {

        exigirCabecalhoAntiCsrf(antiCsrf);
        autenticacaoService.encerrar(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Troca a própria senha (exige a atual) e encerra as outras sessões do usuário")
    public void trocarSenha(
            OperadorAutenticado operador,
            @RequestBody @Valid TrocaSenhaInput input,
            // Autenticada por Bearer; o cookie, se vier, só identifica a sessão deste navegador para mantê-la
            @CookieValue(name = "${app.auth.cookie-name:gomez_refresh}", required = false) String refreshToken) {

        autenticacaoService.trocarSenha(operador.email(), input.getSenhaAtual(), input.getNovaSenha(), refreshToken);
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    public UsuarioLogadoModel me(OperadorAutenticado operador) {
        return new UsuarioLogadoModel(operador.nome(), operador.email(), operador.perfil());
    }

    private ResponseEntity<TokenAcessoModel> responder(ResultadoAutenticacao resultado) {
        Usuario usuario = resultado.usuario();
        TokenService.TokenEmitido token = tokenService.emitir(usuario);
        TokenAcessoModel corpo = new TokenAcessoModel(
                token.valor(), "Bearer", token.expiraEm(),
                new UsuarioLogadoModel(usuario.getNome(), usuario.getEmail(), usuario.getPerfil()));

        ResponseEntity.BodyBuilder resposta = ResponseEntity.ok();
        if (resultado.refreshToken() != null) {
            Duration restante = Duration.between(OffsetDateTime.now(), resultado.sessaoExpiraEm());
            resposta.header(HttpHeaders.SET_COOKIE, cookie(resultado.refreshToken(), restante).toString());
        }
        return resposta.body(corpo);
    }

    private ResponseCookie cookie(String valor, Duration maxAge) {
        return ResponseCookie.from(properties.cookieName(), valor)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(properties.cookiePath())
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }

    /** Um formulário de outro site não consegue enviar este cabeçalho; complementa o SameSite=Strict. */
    private void exigirCabecalhoAntiCsrf(String valor) {
        if (!properties.csrfHeaderValue().equals(valor)) {
            throw new AcessoComercialNegadoException("Requisição de sessão sem o cabeçalho X-Requested-With esperado.");
        }
    }
}
