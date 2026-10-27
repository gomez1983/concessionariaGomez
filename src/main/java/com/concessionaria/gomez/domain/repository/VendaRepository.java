package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long>, JpaSpecificationExecutor<Venda> {

    @Query("from Venda v join fetch v.carro join fetch v.cliente")
    List<Venda> findAllComRelacionamentos();

    Optional<Venda> findByRevisaoConvertidaId(Long revisaoConvertidaId);
}
