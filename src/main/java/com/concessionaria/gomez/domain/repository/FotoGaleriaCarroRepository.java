package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.FotoGaleriaCarro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FotoGaleriaCarroRepository extends JpaRepository<FotoGaleriaCarro, Long> {
    List<FotoGaleriaCarro> findByCarroIdOrderByIdAsc(Long carroId);
    Optional<FotoGaleriaCarro> findByIdAndCarroId(Long id, Long carroId);
}
