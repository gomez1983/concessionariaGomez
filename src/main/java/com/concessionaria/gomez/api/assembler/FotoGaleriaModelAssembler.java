package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.controller.CarroGaleriaController;
import com.concessionaria.gomez.api.model.FotoGaleriaModel;
import com.concessionaria.gomez.domain.model.FotoGaleriaCarro;
import org.springframework.stereotype.Component;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class FotoGaleriaModelAssembler {
    public FotoGaleriaModel toModel(FotoGaleriaCarro foto) {
        FotoGaleriaModel model = new FotoGaleriaModel();
        model.setId(foto.getId());
        model.setDescricao(foto.getDescricao());
        model.setContentType(foto.getContentType());
        model.setTamanho(foto.getTamanho());
        model.add(linkTo(CarroGaleriaController.class, foto.getCarro().getId())
                .slash(foto.getId()).slash("conteudo").withRel("imagem"));
        return model;
    }
}
