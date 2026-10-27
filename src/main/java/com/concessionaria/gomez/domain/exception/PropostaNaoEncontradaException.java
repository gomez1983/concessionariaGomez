package com.concessionaria.gomez.domain.exception;

public class PropostaNaoEncontradaException extends EntidadeNaoEncontradaException {

    public PropostaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }

    public PropostaNaoEncontradaException(Long propostaId) {
        this(String.format("Não existe um cadastro de proposta comercial com código %d", propostaId));
    }
}
