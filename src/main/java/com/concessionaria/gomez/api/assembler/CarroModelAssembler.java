package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.controller.CarroController;
import com.concessionaria.gomez.api.controller.CarroFotoController;
import com.concessionaria.gomez.api.controller.CarroGaleriaController;
import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
@RequiredArgsConstructor
public class CarroModelAssembler {

    private final ModelMapper modelMapper;

    /**
     * @param perfil perfil do operador; o custo de aquisição ({@code compra}) só é exposto
     *               a quem pode ver custos e margens internas (GERENTE/ADMIN).
     */
    public CarroModel toModel(Carro carro, PerfilOperadorComercial perfil) {
        if (carro == null) {
            return null;
        }

        CarroModel carroModel = modelMapper.map(carro, CarroModel.class);
        if (perfil == null || !perfil.podeVerCustosEMargensInternas()) {
            carroModel.setCompra(null);
        }
        carroModel.setVenda(carro.calcularPrecoVendaSugerido());
        carroModel.setPrecoAnunciado(carro.obterPrecoAnunciadoEfetivo());
        carroModel.setZeroKm(carro.isZeroKm());
        carroModel.setDisponibilidadeComercial(
                carro.getDisponibilidadeComercial() != null
                        ? carro.getDisponibilidadeComercial().name()
                        : "DISPONIVEL");
        carroModel.setDiasEmEstoque(carro.calcularDiasEmEstoque(OffsetDateTime.now()));

        // HATEOAS: Link para o próprio recurso individual (self) -> GET /carros/{id}
        if (carro.getId() != null) {
            carroModel.add(linkTo(methodOn(CarroController.class).buscar(carro.getId(), null)).withSelfRel());
            // HATEOAS: Link para foto do veículo -> GET /carros/{id}/foto
            carroModel.add(linkTo(methodOn(CarroFotoController.class).buscar(carro.getId())).withRel("foto"));
            carroModel.add(linkTo(CarroGaleriaController.class, carro.getId()).withRel("galeria"));
        }

        // HATEOAS: Link para a coleção geral de carros -> GET /carros
        carroModel.add(linkTo(CarroController.class).withRel("carros"));

        // HATEOAS: Links dinâmicos de transição de estado de negócio (Ativar / Inativar) -> /carros/{id}/ativo
        if (carro.getId() != null) {
            if (Boolean.TRUE.equals(carro.getAtivo())) {
                carroModel.add(linkTo(CarroController.class)
                        .slash(carro.getId())
                        .slash("ativo")
                        .withRel("inativar"));
            } else {
                carroModel.add(linkTo(CarroController.class)
                        .slash(carro.getId())
                        .slash("ativo")
                        .withRel("ativar"));
            }
        }

        return carroModel;
    }

    public List<CarroModel> toCollectionModel(List<Carro> carros, PerfilOperadorComercial perfil) {
        return carros.stream()
                .map(carro -> toModel(carro, perfil))
                .collect(Collectors.toList());
    }
}
