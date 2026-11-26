package com.concessionaria.gomez.core.security;

import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.domain.model.Usuario;
import com.concessionaria.gomez.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CargaInicialUsuariosTest {

    private static final String EMAIL = "gerente@concessionariagomez.com.br";

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks CargaInicialUsuarios carga;

    @Test
    @DisplayName("Cria o usuário inexistente com a senha codificada, nunca em texto puro")
    void criaUsuarioInexistente() {
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("senha-forte-123")).thenReturn("{bcrypt}hash");

        carga.criarSeNecessario(EMAIL, "Gerente Comercial", PerfilOperadorComercial.GERENTE, "senha-forte-123");

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertThat(salvo.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(salvo.getValue().getPerfil()).isEqualTo(PerfilOperadorComercial.GERENTE);
        assertThat(salvo.getValue().getSenhaHash()).isEqualTo("{bcrypt}hash");
        assertThat(salvo.getValue().getAtivo()).isTrue();
    }

    @Test
    @DisplayName("Usuário já existente não é alterado nem tem a senha redefinida")
    void naoSobrescreveUsuarioExistente() {
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        carga.criarSeNecessario(EMAIL, "Gerente Comercial", PerfilOperadorComercial.GERENTE, "outra-senha-forte");

        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Senha vazia, curta ou acima de 72 bytes não cria usuário")
    void senhaInvalidaNaoCriaUsuario() {
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);

        for (String senha : new String[]{null, "", "   ", "curta", "ç".repeat(40)}) {
            carga.criarSeNecessario(EMAIL, "Gerente Comercial", PerfilOperadorComercial.GERENTE, senha);
        }

        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
