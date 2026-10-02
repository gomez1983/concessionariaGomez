package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.Carro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CarroRepository extends JpaRepository<Carro, Long> {

    List<Carro> findByAno(Integer ano);

    @Query("from Carro c where "
            + "(:ano is null or c.ano = :ano) and "
            + "(:minValor is null or c.compra >= :minValor) and "
            + "(:maxValor is null or c.compra <= :maxValor)")
    List<Carro> findComFiltros(
            @Param("ano") Integer ano,
            @Param("minValor") BigDecimal minValor,
            @Param("maxValor") BigDecimal maxValor);

}
