package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.Carro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Consultas com filtros dinâmicos ficam em {@code CarroSpecs}, aplicadas pelo {@code CarroController},
 * que valida quais filtros cada perfil pode usar.
 */
@Repository
public interface CarroRepository extends JpaRepository<Carro, Long>, JpaSpecificationExecutor<Carro> {

    List<Carro> findByAno(Integer ano);

}
