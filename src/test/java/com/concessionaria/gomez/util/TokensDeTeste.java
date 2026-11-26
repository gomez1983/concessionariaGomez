package com.concessionaria.gomez.util;

import com.concessionaria.gomez.core.security.TokenService;
import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Emite access tokens reais (assinados com o segredo de teste) para os testes de integração,
 * sem depender de usuário gravado no banco.
 */
@Component
@RequiredArgsConstructor
public class TokensDeTeste {

    public static final String EMAIL_VENDEDOR = "vendedor@teste.gomez";
    public static final String EMAIL_GERENTE = "gerente@teste.gomez";
    public static final String EMAIL_ADMIN = "admin@teste.gomez";

    private final TokenService tokenService;

    public String para(PerfilOperadorComercial perfil, String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome("Teste " + perfil.name());
        usuario.setPerfil(perfil);
        return tokenService.emitir(usuario).valor();
    }

    public String para(PerfilOperadorComercial perfil) {
        return para(perfil, switch (perfil) {
            case VENDEDOR -> EMAIL_VENDEDOR;
            case GERENTE -> EMAIL_GERENTE;
            case ADMIN -> EMAIL_ADMIN;
        });
    }

    public String vendedor() {
        return para(PerfilOperadorComercial.VENDEDOR);
    }

    public String gerente() {
        return para(PerfilOperadorComercial.GERENTE);
    }

    public String admin() {
        return para(PerfilOperadorComercial.ADMIN);
    }

    public String bearer(PerfilOperadorComercial perfil) {
        return "Bearer " + para(perfil);
    }
}
