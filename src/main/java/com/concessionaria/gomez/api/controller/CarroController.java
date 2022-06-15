package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.assembler.CarroInputDisassembler;
import com.concessionaria.gomez.api.assembler.CarroModelAssembler;
import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.api.model.input.CarroInput;
import com.concessionaria.gomez.domain.exception.CarroNaoEncontradoException;
import com.concessionaria.gomez.domain.exception.EntidadeEmUsoException;
import com.concessionaria.gomez.domain.exception.EntidadeNaoEncontradaException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import com.concessionaria.gomez.domain.service.CadastroCarroService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/carros")
@Api(value = "API REST Concessionária Gomez")
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
    @ApiOperation(value = "Método para listar veículos")
    public List<CarroModel> listar() {
        return carroModelAssembler.toCollectionModel(carroRepository.findAll());
    }

    @GetMapping("/{carroId}")
    @ApiOperation(value = "Retorna um veículo único")
    public CarroModel buscar(@PathVariable Long carroId) {
        Carro carro = cadastroCarro.buscarOuFalhar(carroId);

        return carroModelAssembler.toModel(carro);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Salva um veículo")
    public CarroModel adicionar(@RequestBody @Valid CarroInput carroInput) {
        try {
            Carro carro = carroInputDisassembler.toDomainObject(carroInput);

            return carroModelAssembler.toModel(cadastroCarro.salvar(carro));
        } catch (CarroNaoEncontradoException e) {
            throw new NegocioException(e.getMessage());
        }
    }
    @PutMapping("/{carroId}")
    @ApiOperation(value = "Altera dados de um veículo")
    public CarroModel atualizar(@PathVariable Long carroId,
                                @RequestBody @Valid CarroInput carroInput) {
        try {
            Carro carroAtual = cadastroCarro.buscarOuFalhar(carroId);

            // 1
            Carro carro = carroInputDisassembler.toDomainObject(carroInput);
            // 2
            BeanUtils.copyProperties(carro, carroAtual, "id");

            return carroModelAssembler.toModel(cadastroCarro.salvar(carroAtual));
        } catch (CarroNaoEncontradoException e) {
            throw new NegocioException(e.getMessage());
        }
    }

    @DeleteMapping("/{carroId}")
    @ApiOperation(value = "Remove um veículo")
    public void remover (@PathVariable Long carroId){
        cadastroCarro.excluir(carroId);
    }
}