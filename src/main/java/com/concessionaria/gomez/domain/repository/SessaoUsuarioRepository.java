package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.SessaoUsuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessaoUsuarioRepository extends JpaRepository<SessaoUsuario, Long> {

    /** Bloqueia a linha: duas renovações simultâneas com o mesmo token são serializadas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoUsuario s join fetch s.usuario where s.tokenHash = :tokenHash")
    Optional<SessaoUsuario> findByTokenHashParaAtualizacao(String tokenHash);

    List<SessaoUsuario> findByUsuarioIdAndRevogadaEmIsNull(Long usuarioId);
}
