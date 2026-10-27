package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.CustoVeiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustoVeiculoRepository extends JpaRepository<CustoVeiculo, Long> {

    List<CustoVeiculo> findByCarroIdOrderByIdAsc(Long carroId);
}
