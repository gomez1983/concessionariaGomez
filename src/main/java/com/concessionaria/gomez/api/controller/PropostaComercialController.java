package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.assembler.PropostaComercialAssembler;
import com.concessionaria.gomez.api.assembler.VendaModelAssembler;
import com.concessionaria.gomez.api.model.PropostaComercialModel;
import com.concessionaria.gomez.api.model.SimulacaoPropostaModel;
import com.concessionaria.gomez.api.model.VendaModel;
import com.concessionaria.gomez.api.model.input.DecisaoAprovacaoInput;
import com.concessionaria.gomez.api.model.input.PoliticaComercialInput;
import com.concessionaria.gomez.api.model.input.PropostaComercialInput;
import com.concessionaria.gomez.domain.exception.AcessoComercialNegadoException;
import com.concessionaria.gomez.domain.model.*;
import com.concessionaria.gomez.domain.repository.PoliticaComercialRepository;
import com.concessionaria.gomez.domain.repository.PropostaComercialRepository;
import com.concessionaria.gomez.domain.service.CadastroCarroService;
import com.concessionaria.gomez.domain.service.GestaoPropostaComercialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/propostas")
@Tag(name = "Propostas Comerciais", description = "Simulação, negociação assistida, alçadas de aprovação e conversão em venda")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PropostaComercialController {

    private final PropostaComercialRepository propostaRepository;
    private final PoliticaComercialRepository politicaRepository;
    private final CadastroCarroService cadastroCarroService;
    private final GestaoPropostaComercialService gestaoPropostaService;
    private final PropostaComercialAssembler assembler;
    private final VendaModelAssembler vendaAssembler;

    @GetMapping
    @Operation(summary = "Lista propostas comerciais com filtro por status, vendedor, clienteId e carroId")
    public Page<PropostaComercialModel> listar(
            @RequestParam(required = false) StatusPropostaComercial status,
            @RequestParam(required = false) String vendedor,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long carroId,
            @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);

        Specification<PropostaComercial> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (vendedor != null && !vendedor.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("vendedorIdentificador")),
                        "%" + vendedor.trim().toLowerCase() + "%"));
            }
            if (clienteId != null) {
                predicates.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (carroId != null) {
                predicates.add(cb.equal(root.get("carro").get("id"), carroId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return propostaRepository.findAll(spec, pageable)
                .map(p -> assembler.toModel(p, gestaoPropostaService.buscarRevisaoAtual(p), perfil));
    }

    @GetMapping("/{propostaId}")
    @Operation(summary = "Busca os detalhes de uma proposta comercial por ID com revisão atual e resumo para cliente")
    public PropostaComercialModel buscar(
            @PathVariable Long propostaId,
            @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader) {

        PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);
        PropostaComercial proposta = gestaoPropostaService.buscarOuFalhar(propostaId);
        RevisaoProposta revisaoAtual = gestaoPropostaService.buscarRevisaoAtual(proposta);
        return assembler.toModel(proposta, revisaoAtual, perfil);
    }

    @PostMapping("/simulacoes")
    @Operation(summary = "Simula em tempo real o cenário financeiro, pisos e alçada de aprovação sem gravar no banco")
    public SimulacaoPropostaModel simular(
            @RequestBody @Valid PropostaComercialInput input,
            @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader,
            @RequestHeader(value = "X-Operador-Id", required = false) String operadorId) {

        PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);
        var command = assembler.toCommand(input, operadorId);
        var analise = gestaoPropostaService.simular(command);
        return assembler.toSimulacaoModel(analise, perfil);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma nova proposta comercial com sua primeira revisão")
    public PropostaComercialModel criar(
            @RequestBody @Valid PropostaComercialInput input,
            @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader,
            @RequestHeader(value = "X-Operador-Id", required = false) String operadorId) {

        PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);
        var command = assembler.toCommand(input, operadorId);
        PropostaComercial proposta = gestaoPropostaService.criarProposta(command);
        return assembler.toModel(proposta, gestaoPropostaService.buscarRevisaoAtual(proposta), perfil);
    }

    @PostMapping("/{propostaId}/revisoes")
    @Operation(summary = "Adiciona uma nova revisão versionada à proposta, invalidando aprovações anteriores")
    public PropostaComercialModel criarRevisao(
            @PathVariable Long propostaId,
            @RequestBody @Valid PropostaComercialInput input,
            @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader,
            @RequestHeader(value = "X-Operador-Id", required = false) String operadorId) {

        PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);
        var command = assembler.toCommand(input, operadorId);
        PropostaComercial proposta = gestaoPropostaService.criarNovaRevisao(propostaId, command);
        return assembler.toModel(proposta, gestaoPropostaService.buscarRevisaoAtual(proposta), perfil);
    }

    @PostMapping("/{propostaId}/aprovacoes")
    @Operation(summary = "Registra decisão gerencial (aprovação ou rejeição) sobre a revisão atual da proposta")
    public PropostaComercialModel decidirAprovacao(
            @PathVariable Long propostaId,
            @RequestBody @Valid DecisaoAprovacaoInput input,
            @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader,
            @RequestHeader(value = "X-Operador-Id", required = false) String operadorId) {

        PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);
        PropostaComercial proposta = gestaoPropostaService.decidirAprovacaoGerencial(
                propostaId,
                input.getSequenciaRevisao(),
                operadorId,
                perfil,
                Boolean.TRUE.equals(input.getAprovar()),
                input.getMotivo());

        return assembler.toModel(proposta, gestaoPropostaService.buscarRevisaoAtual(proposta), perfil);
    }

    @PostMapping("/{propostaId}/reservar")
    @Operation(summary = "Reserva temporariamente o veículo para a proposta comercial")
    public PropostaComercialModel reservar(
            @PathVariable Long propostaId,
            @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader,
            @RequestHeader(value = "X-Operador-Id", required = false) String operadorId) {

        PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);
        PropostaComercial proposta = gestaoPropostaService.reservarVeiculoTemporariamente(propostaId, operadorId, 48);
        return assembler.toModel(proposta, gestaoPropostaService.buscarRevisaoAtual(proposta), perfil);
    }

    @PostMapping("/{propostaId}/converter-venda")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Converte de forma idempotente e transacional uma proposta aprovada em Venda")
    public VendaModel converterEmVenda(
            @PathVariable Long propostaId,
            @RequestParam(required = false) Integer sequenciaRevisao,
            @RequestParam(required = false) String observacoes) {

        Venda venda = gestaoPropostaService.converterEmVenda(propostaId, sequenciaRevisao, observacoes);
        return vendaAssembler.toModel(venda);
    }

    @GetMapping("/politica-vigente")
    @Operation(summary = "Consulta a política comercial vigente para a loja ou para um veículo específico")
    public PoliticaComercial consultarPoliticaVigente(@RequestParam(required = false) Long carroId) {
        OffsetDateTime agora = OffsetDateTime.now();
        if (carroId != null) {
            var excecoes = politicaRepository.findVigentes(ContextoPoliticaComercial.EXCECAO_VEICULO, carroId, agora);
            if (!excecoes.isEmpty()) {
                return excecoes.get(0);
            }
        }
        var padroes = politicaRepository.findVigentes(ContextoPoliticaComercial.LOJA_PADRAO, null, agora);
        return padroes.isEmpty() ? null : padroes.get(0);
    }

    @PostMapping("/politicas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova versão de política comercial (restrito a GERENTE ou ADMIN)")
    public PoliticaComercial cadastrarPolitica(
            @RequestBody @Valid PoliticaComercialInput input,
            @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader,
            @RequestHeader(value = "X-Operador-Id", required = false) String operadorId) {

        PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);
        if (!perfil.podeConfigurarPolitica()) {
            throw new AcessoComercialNegadoException("Apenas GERENTE ou ADMIN podem configurar políticas comerciais.");
        }

        PoliticaComercial nova = new PoliticaComercial();
        nova.setContexto(input.getContexto());
        if (input.getCarroId() != null) {
            nova.setCarro(cadastroCarroService.buscarOuFalhar(input.getCarroId()));
        }
        int proximaVersao = politicaRepository.findAll().stream()
                .mapToInt(p -> p.getVersao() != null ? p.getVersao() : 1)
                .max().orElse(0) + 1;
        nova.setVersao(proximaVersao);
        nova.setMargemDesejada(input.getMargemDesejada());
        nova.setMargemAutonomia(input.getMargemAutonomia());
        nova.setMargemPisoAbsoluto(input.getMargemPisoAbsoluto());
        nova.setDescontoMaximoAutonomiaReais(input.getDescontoMaximoAutonomiaReais());
        nova.setDescontoMaximoAutonomiaPercentual(input.getDescontoMaximoAutonomiaPercentual());
        nova.setResponsavelCadastro(operadorId != null && !operadorId.isBlank()
                ? operadorId.trim()
                : "gestor@concessionariagomez.com.br");
        nova.setVigenteAPartirDe(OffsetDateTime.now().minusSeconds(1));
        nova.setAtiva(Boolean.TRUE);
        nova.validarInvariantes();

        return politicaRepository.save(nova);
    }
}
