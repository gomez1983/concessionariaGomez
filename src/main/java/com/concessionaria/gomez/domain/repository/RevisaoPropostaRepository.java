package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.RevisaoProposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RevisaoPropostaRepository extends JpaRepository<RevisaoProposta, Long> {

    List<RevisaoProposta> findByPropostaIdOrderBySequenciaAsc(Long propostaId);

    Optional<RevisaoProposta> findByPropostaIdAndSequencia(Long propostaId, Integer sequencia);
}
