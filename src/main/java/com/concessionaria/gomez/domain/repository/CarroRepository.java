package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.Carro;

import java.util.List;

public interface CarroRepository {

    List<Carro> listar();
    Carro buscar(Long id);
    Carro salvar(Carro cozinha);
    void remover(Carro cozinha);
}
