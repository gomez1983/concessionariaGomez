package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.CampanhaComercial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampanhaComercialRepository extends JpaRepository<CampanhaComercial, Long> {

    Optional<CampanhaComercial> findByCodigo(String codigo);
}
