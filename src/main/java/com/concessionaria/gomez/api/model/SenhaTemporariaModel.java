package com.concessionaria.gomez.api.model;

/**
 * Resposta que carrega uma senha temporária. Ela aparece só nesta resposta (enviada com Cache-Control: no-store)
 * e não é guardada: repasse-a ao usuário por um canal seguro e peça que ele a troque em "Trocar senha".
 */
public record SenhaTemporariaModel(UsuarioModel usuario, String senhaTemporaria) {
}
