package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.assembler.CarroInputDisassembler;
import com.concessionaria.gomez.api.assembler.CarroModelAssembler;
import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.api.model.input.CarroInput;
import com.concessionaria.gomez.core.security.OperadorAutenticado;
import com.concessionaria.gomez.domain.exception.AcessoComercialNegadoException;
import com.concessionaria.gomez.domain.exception.CarroNaoEncontradoException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.filter.CarroFilter;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import com.concessionaria.gomez.domain.service.CadastroCarroService;
import com.concessionaria.gomez.infrastructure.repository.spec.CarroSpecs;
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
@RequestMapping(value = "/carros")
@Tag(name = "Carros", description = "Gerenciamento de veículos da Concessionária Gomez")
@RequiredArgsConstructor
public class CarroController {

    private static final String PROPRIEDADE_CUSTO = "compra";

    private final CarroRepository carroRepository;
    private final CadastroCarroService cadastroCarro;
    private final CarroModelAssembler carroModelAssembler;
    private final CarroInputDisassembler carroInputDisassembler;

    @GetMapping
    @Operation(summary = "Método para listar veículos (com filtros dinâmicos via Specification, paginação e ordenação)",
            description = "Os filtros minValor/maxValor e a ordenação por 'compra' atuam sobre o custo de aquisição "
                    + "e são restritos a GERENTE ou ADMIN.")
    public Page<CarroModel> listar(
            @ParameterObject CarroFilter filtro,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable,
            OperadorAutenticado operador) {

        PerfilOperadorComercial perfil = operador.perfil();
        validarConsultaPorCusto(filtro, pageable, perfil);

        Page<Carro> carrosPage = carroRepository.findAll(CarroSpecs.usandoFiltro(filtro), pageable);

        return carrosPage.map(carro -> carroModelAssembler.toModel(carro, perfil));
    }

    @GetMapping("/{carroId}")
    @Operation(summary = "Retorna um veículo único com links HATEOAS")
    public CarroModel buscar(
            @PathVariable Long carroId,
            OperadorAutenticado operador) {
        Carro carro = cadastroCarro.buscarOuFalhar(carroId);

        return carroModelAssembler.toModel(carro, operador.perfil());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Salva um veículo")
    public CarroModel adicionar(
            @RequestBody @Valid CarroInput carroInput,
            OperadorAutenticado operador) {
        try {
            Carro carro = carroInputDisassembler.toDomainObject(carroInput);

            return carroModelAssembler.toModel(cadastroCarro.salvar(carro),
                    operador.perfil());
        } catch (CarroNaoEncontradoException e) {
            throw new NegocioException(e.getMessage());
        }
    }

    @PutMapping("/{carroId}")
    @Operation(summary = "Altera dados de um veículo")
    public CarroModel atualizar(@PathVariable Long carroId,
                                @RequestBody @Valid CarroInput carroInput,
                                OperadorAutenticado operador) {
        try {
            Carro carroAtual = cadastroCarro.buscarOuFalhar(carroId);

            carroInputDisassembler.copyToDomainObject(carroInput, carroAtual);

            return carroModelAssembler.toModel(cadastroCarro.salvar(carroAtual),
                    operador.perfil());
        } catch (CarroNaoEncontradoException e) {
            throw new NegocioException(e.getMessage());
        }
    }

    // PUT /carros/{id}/ativo - vai ativar o carro
    // DELETE /carros/{id}/ativo - vai inativar o carro

    @PutMapping("/{carroId}/ativo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ativa um veículo no estoque (veículos com venda vigente exigem o cancelamento da venda)")
    public void ativar(@PathVariable Long carroId) {
        cadastroCarro.ativar(carroId);
    }

    @DeleteMapping("/{carroId}/ativo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa um veículo no estoque")
    public void inativar(@PathVariable Long carroId) {
        cadastroCarro.inativar(carroId);
    }

    @DeleteMapping("/{carroId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um veículo")
    public void remover (@PathVariable Long carroId){
        cadastroCarro.excluir(carroId);
    }

    /**
     * Filtrar ou ordenar pelo custo de aquisição permitiria deduzir o valor oculto
     * (por exemplo, por busca binária em minValor/maxValor). Restrito a quem pode ver custos.
     */
    private void validarConsultaPorCusto(CarroFilter filtro, Pageable pageable, PerfilOperadorComercial perfil) {
        if (perfil.podeVerCustosEMargensInternas()) {
            return;
        }
        boolean filtraPorCusto = filtro != null && (filtro.getMinValor() != null || filtro.getMaxValor() != null);
        boolean ordenaPorCusto = pageable.getSort().getOrderFor(PROPRIEDADE_CUSTO) != null;
        if (filtraPorCusto || ordenaPorCusto) {
            throw new AcessoComercialNegadoException(
                    "Filtros e ordenação por valor de compra são restritos a GERENTE ou ADMIN.");
        }
    }
}
