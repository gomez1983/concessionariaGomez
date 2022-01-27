package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.domain.exception.EntidadeEmUsoException;
import com.concessionaria.gomez.domain.exception.EntidadeNaoEncontradaException;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import com.concessionaria.gomez.domain.service.CadastroCarroService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/carros")
public class CarroController {

    @Autowired
    private CarroRepository carroRepository;

    @Autowired
    private CadastroCarroService cadastroCarro;

    @GetMapping
    public List<Carro> listar() {
        return carroRepository.findAll();
    }

    @GetMapping("/{cozinhaId}")
    public ResponseEntity<Carro> buscar(@PathVariable Long carroId) {
        Optional<Carro> carro = carroRepository.findById(carroId);

        if (carro.isPresent()) {
            return ResponseEntity.ok(carro.get());
        }

        return ResponseEntity.notFound().build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Carro adicionar(@RequestBody Carro carro) {
        return cadastroCarro.salvar(carro);
    }

    @PutMapping("/{cozinhaId}")
    public ResponseEntity<Carro> atualizar(@PathVariable Long carroId,
                                             @RequestBody Carro carro) {
        Optional<Carro> carroAtual = carroRepository.findById(carroId);

        if (carroAtual.isPresent()) {
            BeanUtils.copyProperties(carro, carroAtual.get(), "id");

            Carro carroSalvo = cadastroCarro.salvar(carroAtual.get());
            return ResponseEntity.ok(carroSalvo);
        }

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{carroId}")
    public ResponseEntity<?> remover(@PathVariable Long carroId) {
        try {
            cadastroCarro.excluir(carroId);
            return ResponseEntity.noContent().build();

        } catch (EntidadeNaoEncontradaException e) {
            return ResponseEntity.notFound().build();

        } catch (EntidadeEmUsoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

}