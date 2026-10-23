package com.concessionaria.gomez.domain.exception;

public class FotoCarroNaoEncontradaException extends EntidadeNaoEncontradaException {

    private static final long serialVersionUID = 1L;

    public FotoCarroNaoEncontradaException(String mensagem) {
        super(mensagem);
    }

    public FotoCarroNaoEncontradaException(Long carroId) {
        this(String.format("Não existe um cadastro de foto para o veículo com código %d", carroId));
    }
}
