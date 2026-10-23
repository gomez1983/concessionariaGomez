package com.concessionaria.gomez.domain.exception;

public class VendaNaoEncontradaException extends EntidadeNaoEncontradaException {

    private static final long serialVersionUID = 1L;

    public VendaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }

    public VendaNaoEncontradaException(Long vendaId) {
        this(String.format("Não existe um registro de venda com código %d", vendaId));
    }
}
