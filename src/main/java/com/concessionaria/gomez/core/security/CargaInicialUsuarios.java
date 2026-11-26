package com.concessionaria.gomez.core.security;

import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.domain.model.Usuario;
import com.concessionaria.gomez.domain.repository.UsuarioRepository;
import com.concessionaria.gomez.domain.service.PoliticaSenha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria os usuários iniciais a partir de senhas do {@code .env}: nenhuma senha fica no repositório.
 * Usa os mesmos e-mails já gravados no histórico das propostas e vendas.
 * Só cria usuário inexistente; nunca altera nem redefine a senha de quem já existe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CargaInicialUsuarios implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:}")
    private String senhaAdmin;

    @Value("${app.seed.gerente-password:}")
    private String senhaGerente;

    @Value("${app.seed.vendedor-password:}")
    private String senhaVendedor;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        criarSeNecessario("admin@concessionariagomez.com.br", "Administrador", PerfilOperadorComercial.ADMIN, senhaAdmin);
        criarSeNecessario("gerente@concessionariagomez.com.br", "Gerente Comercial", PerfilOperadorComercial.GERENTE, senhaGerente);
        criarSeNecessario("vendedor@concessionariagomez.com.br", "Vendedor", PerfilOperadorComercial.VENDEDOR, senhaVendedor);
    }

    void criarSeNecessario(String email, String nome, PerfilOperadorComercial perfil, String senha) {
        if (senha == null || senha.isBlank() || usuarioRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        if (!PoliticaSenha.ehValida(senha)) {
            log.warn("Usuário inicial {} não criado: a senha deve ter entre {} e {} caracteres (até 72 bytes).",
                    email, PoliticaSenha.TAMANHO_MINIMO, PoliticaSenha.TAMANHO_MAXIMO);
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome(nome);
        usuario.setPerfil(perfil);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuarioRepository.save(usuario);
        log.info("Usuário inicial criado: {} ({}).", email, perfil);
    }
}
