package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.ContextoPoliticaComercial;
import com.concessionaria.gomez.domain.model.PoliticaComercial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PoliticaComercialRepository extends JpaRepository<PoliticaComercial, Long> {

    @Query("""
            select p from PoliticaComercial p
            where p.ativa = true
              and p.contexto = :contexto
              and (:carroId is null or p.carro.id = :carroId)
              and p.vigenteAPartirDe <= :instante
              and (p.vigenteAte is null or p.vigenteAte >= :instante)
            order by p.versao desc, p.vigenteAPartirDe desc, p.id desc
            """)
    List<PoliticaComercial> findVigentes(
            @Param("contexto") ContextoPoliticaComercial contexto,
            @Param("carroId") Long carroId,
            @Param("instante") OffsetDateTime instante);
}
