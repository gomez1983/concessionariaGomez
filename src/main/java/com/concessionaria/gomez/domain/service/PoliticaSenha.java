package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.NegocioException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Regras de senha do painel. O limite superior vem do BCrypt, que só aceita até 72 bytes (UTF-8):
 * uma senha com acentos pode ter menos de 72 caracteres e ainda assim passar do limite.
 */
public final class PoliticaSenha {

    public static final int TAMANHO_MINIMO = 10;
    public static final int TAMANHO_MAXIMO = 72;
    private static final int LIMITE_BCRYPT_BYTES = 72;

    /** Sem caracteres ambíguos (0/O, 1/l/I), para a senha temporária ser ditada ou copiada sem erro. */
    private static final String ALFABETO_TEMPORARIA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TAMANHO_TEMPORARIA = 16;
    private static final SecureRandom GERADOR = new SecureRandom();

    public static final String MSG_SENHA_INVALIDA = String.format(
            "A senha deve ter entre %d e %d caracteres.", TAMANHO_MINIMO, TAMANHO_MAXIMO);

    private PoliticaSenha() {
    }

    public static boolean excedeLimiteBcrypt(String senha) {
        return senha.getBytes(StandardCharsets.UTF_8).length > LIMITE_BCRYPT_BYTES;
    }

    public static boolean ehValida(String senha) {
        return senha != null
                && senha.length() >= TAMANHO_MINIMO
                && senha.length() <= TAMANHO_MAXIMO
                && !senha.isBlank()
                && !excedeLimiteBcrypt(senha);
    }

    public static void validar(String senha) {
        if (!ehValida(senha)) {
            throw new NegocioException(MSG_SENHA_INVALIDA);
        }
    }

    /** Senha aleatória de 16 caracteres (cerca de 92 bits), entregue uma única vez ao administrador. */
    public static String gerarTemporaria() {
        StringBuilder senha = new StringBuilder(TAMANHO_TEMPORARIA);
        for (int i = 0; i < TAMANHO_TEMPORARIA; i++) {
            senha.append(ALFABETO_TEMPORARIA.charAt(GERADOR.nextInt(ALFABETO_TEMPORARIA.length())));
        }
        return senha.toString();
    }
}
