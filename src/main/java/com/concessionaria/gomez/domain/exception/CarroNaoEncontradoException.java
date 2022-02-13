package com.concessionaria.gomez.domain.exception;

public class CarroNaoEncontradoException extends EntidadeNaoEncontradaException{

    private static final long serialVersionUID = 1L;

    public CarroNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public CarroNaoEncontradoException(Long carroId) {
        this(String.format("Não existe um cadastro de carro com código %d", carroId));
    }
}