package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.assembler.ClienteInputDisassembler;
import com.concessionaria.gomez.api.assembler.ClienteModelAssembler;
import com.concessionaria.gomez.api.model.ClienteModel;
import com.concessionaria.gomez.api.model.input.ClienteInput;
import com.concessionaria.gomez.domain.model.Cliente;
import com.concessionaria.gomez.domain.repository.ClienteRepository;
import com.concessionaria.gomez.domain.service.CadastroClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Gerenciamento de clientes da Concessionária Gomez")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CadastroClienteService cadastroCliente;

    @Autowired
    private ClienteModelAssembler clienteModelAssembler;

    @Autowired
    private ClienteInputDisassembler clienteInputDisassembler;

    @GetMapping
    @Operation(summary = "Lista todos os clientes com paginação e ordenação")
    public Page<ClienteModel> listar(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        Page<Cliente> clientesPage = clienteRepository.findAll(pageable);
        return clientesPage.map(clienteModelAssembler::toModel);
    }

    @GetMapping("/{clienteId}")
    @Operation(summary = "Busca um cliente por ID com links HATEOAS")
    public ClienteModel buscar(@PathVariable Long clienteId) {
        Cliente cliente = cadastroCliente.buscarOuFalhar(clienteId);
        return clienteModelAssembler.toModel(cliente);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo cliente")
    public ClienteModel adicionar(@RequestBody @Valid ClienteInput clienteInput) {
        Cliente cliente = clienteInputDisassembler.toDomainObject(clienteInput);
        return clienteModelAssembler.toModel(cadastroCliente.salvar(cliente));
    }

    @PutMapping("/{clienteId}")
    @Operation(summary = "Atualiza os dados de um cliente existente")
    public ClienteModel atualizar(@PathVariable Long clienteId,
                                  @RequestBody @Valid ClienteInput clienteInput) {
        Cliente clienteAtual = cadastroCliente.buscarOuFalhar(clienteId);
        clienteInputDisassembler.copyToDomainObject(clienteInput, clienteAtual);
        return clienteModelAssembler.toModel(cadastroCliente.salvar(clienteAtual));
    }

    @PutMapping("/{clienteId}/ativo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ativa um cliente no sistema")
    public void ativar(@PathVariable Long clienteId) {
        cadastroCliente.ativar(clienteId);
    }

    @DeleteMapping("/{clienteId}/ativo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa um cliente no sistema")
    public void inativar(@PathVariable Long clienteId) {
        cadastroCliente.inativar(clienteId);
    }

    @DeleteMapping("/{clienteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um cliente por ID")
    public void remover(@PathVariable Long clienteId) {
        cadastroCliente.excluir(clienteId);
    }
}
