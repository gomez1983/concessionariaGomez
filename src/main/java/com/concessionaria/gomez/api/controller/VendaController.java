package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.assembler.VendaModelAssembler;
import com.concessionaria.gomez.api.model.VendaModel;
import com.concessionaria.gomez.api.model.input.VendaInput;
import com.concessionaria.gomez.domain.model.Venda;
import com.concessionaria.gomez.domain.repository.VendaRepository;
import com.concessionaria.gomez.domain.service.EmissaoVendaService;
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
@RequestMapping("/vendas")
@Tag(name = "Vendas", description = "Gerenciamento e emissão de vendas de veículos")
@CrossOrigin(origins = "*")
public class VendaController {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private EmissaoVendaService emissaoVendaService;

    @Autowired
    private VendaModelAssembler vendaModelAssembler;

    @GetMapping
    @Operation(summary = "Lista todas as vendas realizadas com paginação e ordenação")
    public Page<VendaModel> listar(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        Page<Venda> vendasPage = vendaRepository.findAll(pageable);
        return vendasPage.map(vendaModelAssembler::toModel);
    }

    @GetMapping("/{vendaId}")
    @Operation(summary = "Busca os detalhes de uma venda por ID com links HATEOAS")
    public VendaModel buscar(@PathVariable Long vendaId) {
        Venda venda = emissaoVendaService.buscarOuFalhar(vendaId);
        return vendaModelAssembler.toModel(venda);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma nova venda, associando veículo ao cliente e inativando o carro no estoque")
    public VendaModel realizarVenda(@RequestBody @Valid VendaInput vendaInput) {
        Venda venda = emissaoVendaService.realizarVenda(
                vendaInput.getCarroId(),
                vendaInput.getClienteId(),
                vendaInput.getValorVenda(),
                vendaInput.getObservacoes()
        );
        return vendaModelAssembler.toModel(venda);
    }

    @DeleteMapping("/{vendaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela uma venda e reativa o veículo no estoque")
    public void cancelar(@PathVariable Long vendaId) {
        emissaoVendaService.cancelar(vendaId);
    }
}
