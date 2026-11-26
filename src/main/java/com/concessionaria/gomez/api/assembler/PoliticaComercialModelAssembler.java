package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.PoliticaComercialModel;
import com.concessionaria.gomez.domain.model.PoliticaComercial;
import org.springframework.stereotype.Component;

@Component
public class PoliticaComercialModelAssembler {

    public PoliticaComercialModel toModel(PoliticaComercial politica) {
        if (politica == null) {
            return null;
        }

        PoliticaComercialModel model = new PoliticaComercialModel();
        model.setId(politica.getId());
        model.setContexto(politica.getContexto());
        // getId() de um proxy LAZY devolve o identificador sem carregar o veículo
        model.setCarroId(politica.getCarro() != null ? politica.getCarro().getId() : null);
        model.setVersao(politica.getVersao());
        model.setMargemDesejada(politica.getMargemDesejada());
        model.setMargemAutonomia(politica.getMargemAutonomia());
        model.setMargemPisoAbsoluto(politica.getMargemPisoAbsoluto());
        model.setDescontoMaximoAutonomiaReais(politica.getDescontoMaximoAutonomiaReais());
        model.setDescontoMaximoAutonomiaPercentual(politica.getDescontoMaximoAutonomiaPercentual());
        model.setResponsavelCadastro(politica.getResponsavelCadastro());
        model.setVigenteAPartirDe(politica.getVigenteAPartirDe());
        model.setVigenteAte(politica.getVigenteAte());
        model.setAtiva(politica.getAtiva());
        model.setDataCadastro(politica.getDataCadastro());
        return model;
    }
}
