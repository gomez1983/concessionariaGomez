package com.concessionaria.gomez.domain.exception;

/**
 * Falha de autenticação (HTTP 401). A mensagem é exibida ao usuário, portanto nunca deve revelar
 * se o e-mail existe, se a conta está inativa ou bloqueada.
 */
public class AutenticacaoException extends RuntimeException {

    public static final String MSG_CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos.";
    public static final String MSG_SESSAO_EXPIRADA = "Sua sessão expirou. Entre novamente.";

    public AutenticacaoException(String mensagem) {
        super(mensagem);
    }

    public static AutenticacaoException credenciaisInvalidas() {
        return new AutenticacaoException(MSG_CREDENCIAIS_INVALIDAS);
    }

    public static AutenticacaoException sessaoExpirada() {
        return new AutenticacaoException(MSG_SESSAO_EXPIRADA);
    }
}
