package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.Carro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CarroRepository extends JpaRepository<Carro, Long>, JpaSpecificationExecutor<Carro> {

    List<Carro> findByAno(Integer ano);

    @Query(value = "from Carro c where "
            + "(:ano is null or c.ano = :ano) and "
            + "(:minValor is null or c.compra >= :minValor) and "
            + "(:maxValor is null or c.compra <= :maxValor)",
           countQuery = "select count(c) from Carro c where "
            + "(:ano is null or c.ano = :ano) and "
            + "(:minValor is null or c.compra >= :minValor) and "
            + "(:maxValor is null or c.compra <= :maxValor)")
    Page<Carro> findComFiltros(
            @Param("ano") Integer ano,
            @Param("minValor") BigDecimal minValor,
            @Param("maxValor") BigDecimal maxValor,
            Pageable pageable);

}
