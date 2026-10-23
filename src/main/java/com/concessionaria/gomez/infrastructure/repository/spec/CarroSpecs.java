package com.concessionaria.gomez.infrastructure.repository.spec;

import com.concessionaria.gomez.domain.filter.CarroFilter;
import com.concessionaria.gomez.domain.model.Carro;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

public class CarroSpecs {

    public static Specification<Carro> usandoFiltro(CarroFilter filtro) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            if (filtro == null) {
                return builder.and(predicates.toArray(new Predicate[0]));
            }

            if (StringUtils.hasText(filtro.getMarca())) {
                predicates.add(builder.like(
                        builder.lower(root.get("marca")),
                        "%" + filtro.getMarca().toLowerCase().trim() + "%"
                ));
            }

            if (StringUtils.hasText(filtro.getModelo())) {
                predicates.add(builder.like(
                        builder.lower(root.get("modelo")),
                        "%" + filtro.getModelo().toLowerCase().trim() + "%"
                ));
            }

            if (filtro.getAno() != null) {
                predicates.add(builder.equal(root.get("ano"), filtro.getAno()));
            }

            if (filtro.getMinValor() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("compra"), filtro.getMinValor()));
            }

            if (filtro.getMaxValor() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("compra"), filtro.getMaxValor()));
            }

            if (filtro.getMinQuilometragem() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("quilometragem"), filtro.getMinQuilometragem()));
            }

            if (filtro.getMaxQuilometragem() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("quilometragem"), filtro.getMaxQuilometragem()));
            }

            if (filtro.getAtivo() != null) {
                predicates.add(builder.equal(root.get("ativo"), filtro.getAtivo()));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
