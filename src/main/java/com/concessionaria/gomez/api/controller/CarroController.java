package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.assembler.CarroInputDisassembler;
import com.concessionaria.gomez.api.assembler.CarroModelAssembler;
import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.api.model.input.CarroInput;
import com.concessionaria.gomez.domain.exception.CarroNaoEncontradoException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.filter.CarroFilter;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import com.concessionaria.gomez.domain.service.CadastroCarroService;
import com.concessionaria.gomez.infrastructure.repository.spec.CarroSpecs;
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
@RequestMapping(value = "/carros")
@Tag(name = "Carros", description = "Gerenciamento de veículos da Concessionária Gomez")
@CrossOrigin(origins = "*")
public class CarroController {

    @Autowired
    private CarroRepository carroRepository;

    @Autowired
    private CadastroCarroService cadastroCarro;

    @Autowired
    private CarroModelAssembler carroModelAssembler;

    @Autowired
    private CarroInputDisassembler carroInputDisassembler;

    @GetMapping
    @Operation(summary = "Método para listar veículos (com filtros dinâmicos via Specification, paginação e ordenação)")
    public Page<CarroModel> listar(
            @ParameterObject CarroFilter filtro,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {

        Page<Carro> carrosPage = carroRepository.findAll(CarroSpecs.usandoFiltro(filtro), pageable);

        return carrosPage.map(carroModelAssembler::toModel);
    }

    @GetMapping("/{carroId}")
    @Operation(summary = "Retorna um veículo único com links HATEOAS")
    public CarroModel buscar(@PathVariable Long carroId) {
        Carro carro = cadastroCarro.buscarOuFalhar(carroId);

        return carroModelAssembler.toModel(carro);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Salva um veículo")
    public CarroModel adicionar(@RequestBody @Valid CarroInput carroInput) {
        try {
            Carro carro = carroInputDisassembler.toDomainObject(carroInput);

            return carroModelAssembler.toModel(cadastroCarro.salvar(carro));
        } catch (CarroNaoEncontradoException e) {
            throw new NegocioException(e.getMessage());
        }
    }

    @PutMapping("/{carroId}")
    @Operation(summary = "Altera dados de um veículo")
    public CarroModel atualizar(@PathVariable Long carroId,
                                @RequestBody @Valid CarroInput carroInput) {
        try {
            Carro carroAtual = cadastroCarro.buscarOuFalhar(carroId);

            carroInputDisassembler.copyToDomainObject(carroInput, carroAtual);

            return carroModelAssembler.toModel(cadastroCarro.salvar(carroAtual));
        } catch (CarroNaoEncontradoException e) {
            throw new NegocioException(e.getMessage());
        }
    }

    // PUT /carros/{id}/ativo - vai ativar o carro
    // DELETE /carros/{id}/ativo - vai inativar o carro

    @PutMapping("/{carroId}/ativo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ativa um veículo no estoque")
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
}
