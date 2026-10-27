package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.PropostaComercial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropostaComercialRepository extends JpaRepository<PropostaComercial, Long>,
        JpaSpecificationExecutor<PropostaComercial> {

    Optional<PropostaComercial> findByNumero(String numero);
}
