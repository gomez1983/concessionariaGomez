package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.Carro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarroRepository extends JpaRepository<Carro, Long> {

    List<Carro> findByAno(Integer ano);

}
