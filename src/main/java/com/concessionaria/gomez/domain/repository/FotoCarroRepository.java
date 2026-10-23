package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.FotoCarro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FotoCarroRepository extends JpaRepository<FotoCarro, Long> {

    Optional<FotoCarro> findById(Long carroId);
}
