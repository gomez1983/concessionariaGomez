package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.model.SenhaTemporariaModel;
import com.concessionaria.gomez.api.model.UsuarioModel;
import com.concessionaria.gomez.api.model.input.UsuarioAtualizacaoInput;
import com.concessionaria.gomez.api.model.input.UsuarioInput;
import com.concessionaria.gomez.core.security.OperadorAutenticado;
import com.concessionaria.gomez.domain.repository.UsuarioRepository;
import com.concessionaria.gomez.domain.service.GestaoUsuarioService;
import com.concessionaria.gomez.domain.service.GestaoUsuarioService.UsuarioComSenhaTemporaria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Administração de usuários. Todas as rotas exigem ADMIN (matriz em SecurityConfig). */
@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários", description = "Administração dos usuários do painel (somente ADMIN)")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final GestaoUsuarioService gestaoUsuario;

    @GetMapping
    @Operation(summary = "Lista os usuários com paginação (padrão: ordem alfabética)")
    public Page<UsuarioModel> listar(
            @ParameterObject @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioModel::de);
    }

    @GetMapping("/{usuarioId}")
    @Operation(summary = "Busca um usuário por ID")
    public UsuarioModel buscar(@PathVariable Long usuarioId) {
        return UsuarioModel.de(gestaoUsuario.buscarOuFalhar(usuarioId));
    }

    @PostMapping
    @Operation(summary = "Cadastra um usuário e devolve, uma única vez, a senha temporária gerada")
    public ResponseEntity<SenhaTemporariaModel> cadastrar(@RequestBody @Valid UsuarioInput input) {
        UsuarioComSenhaTemporaria criado = gestaoUsuario.cadastrar(input.getNome(), input.getEmail(), input.getPerfil());
        return respostaComSenha(HttpStatus.CREATED, criado);
    }

    @PutMapping("/{usuarioId}")
    @Operation(summary = "Altera nome e perfil (o e-mail não muda; ninguém altera o próprio perfil)")
    public UsuarioModel atualizar(@PathVariable Long usuarioId,
                                  @RequestBody @Valid UsuarioAtualizacaoInput input,
                                  OperadorAutenticado operador) {
        return UsuarioModel.de(gestaoUsuario.atualizar(usuarioId, input.getNome(), input.getPerfil(), operador.email()));
    }

    @PutMapping("/{usuarioId}/ativo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reativa o usuário e zera o bloqueio por tentativas")
    public void ativar(@PathVariable Long usuarioId) {
        gestaoUsuario.ativar(usuarioId);
    }

    @DeleteMapping("/{usuarioId}/ativo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa o usuário e encerra as sessões dele (ninguém inativa a si mesmo)")
    public void inativar(@PathVariable Long usuarioId, OperadorAutenticado operador) {
        gestaoUsuario.inativar(usuarioId, operador.email());
    }

    @PostMapping("/{usuarioId}/senha-temporaria")
    @Operation(summary = "Gera uma nova senha temporária, zera o bloqueio e encerra as sessões do usuário")
    public ResponseEntity<SenhaTemporariaModel> redefinirSenha(@PathVariable Long usuarioId, OperadorAutenticado operador) {
        return respostaComSenha(HttpStatus.OK, gestaoUsuario.redefinirSenhaTemporaria(usuarioId, operador.email()));
    }

    /** A senha temporária não pode ficar em cache de navegador nem de proxy. */
    private static ResponseEntity<SenhaTemporariaModel> respostaComSenha(HttpStatus status, UsuarioComSenhaTemporaria resultado) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(new SenhaTemporariaModel(UsuarioModel.de(resultado.usuario()), resultado.senhaTemporaria()));
    }
}
