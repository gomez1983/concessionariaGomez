package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.assembler.VendaModelAssembler;
import com.concessionaria.gomez.api.model.VendaModel;
import com.concessionaria.gomez.api.model.input.VendaInput;
import com.concessionaria.gomez.core.security.OperadorAutenticado;
import com.concessionaria.gomez.domain.model.Venda;
import com.concessionaria.gomez.domain.repository.VendaRepository;
import com.concessionaria.gomez.domain.service.EmissaoVendaService;
import com.concessionaria.gomez.domain.service.GestaoPropostaComercialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vendas")
@Tag(name = "Vendas", description = "Gerenciamento e emissão de vendas de veículos")
@RequiredArgsConstructor
public class VendaController {

    private final VendaRepository vendaRepository;
    private final EmissaoVendaService emissaoVendaService;
    private final GestaoPropostaComercialService gestaoPropostaService;
    private final VendaModelAssembler vendaModelAssembler;

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
    @Operation(summary = "Registra uma nova venda (por conversão de proposta autorizada ou emissão direta) e inativa o carro no estoque")
    public VendaModel realizarVenda(@RequestBody @Valid VendaInput vendaInput, OperadorAutenticado operador) {
        Venda venda;
        if (vendaInput.getPropostaId() != null && gestaoPropostaService != null) {
            venda = gestaoPropostaService.converterEmVenda(
                    vendaInput.getPropostaId(),
                    vendaInput.getSequenciaRevisaoAutorizada(),
                    vendaInput.getObservacoes(),
                    operador.email(),
                    operador.perfil());
        } else {
            venda = emissaoVendaService.realizarVenda(
                    vendaInput.getCarroId(),
                    vendaInput.getClienteId(),
                    vendaInput.getValorVenda(),
                    vendaInput.getObservacoes()
            );
        }
        return vendaModelAssembler.toModel(venda);
    }

    @PutMapping("/{vendaId}")
    @Operation(summary = "Atualiza os dados de uma venda existente")
    public VendaModel atualizar(@PathVariable Long vendaId, @RequestBody @Valid VendaInput vendaInput) {
        Venda venda = emissaoVendaService.atualizar(
                vendaId,
                vendaInput.getCarroId(),
                vendaInput.getClienteId(),
                vendaInput.getValorVenda(),
                vendaInput.getObservacoes()
        );
        return vendaModelAssembler.toModel(venda);
    }

    @PostMapping("/{vendaId}/cancelamento-auditado")
    @Operation(summary = "Cancela uma venda mantendo registro auditado e controlando a reativação do veículo no estoque")
    public VendaModel cancelarAuditado(
            @PathVariable Long vendaId,
            @RequestParam String motivo,
            @RequestParam(defaultValue = "true") boolean reativarVeiculo,
            OperadorAutenticado operador) {

        Venda venda = emissaoVendaService.cancelarComAuditoria(vendaId, motivo, operador.email(), reativarVeiculo);
        return vendaModelAssembler.toModel(venda);
    }

    @DeleteMapping("/{vendaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma venda legada e reativa o veículo no estoque (sem trilha de auditoria)",
            description = "Obsoleto: use POST /vendas/{vendaId}/cancelamento-auditado. Recusa vendas canceladas "
                    + "e vendas originadas de proposta.",
            deprecated = true)
    public void cancelar(@PathVariable Long vendaId) {
        emissaoVendaService.cancelar(vendaId);
    }
}
