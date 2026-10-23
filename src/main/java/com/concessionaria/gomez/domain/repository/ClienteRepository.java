package com.concessionaria.gomez.domain.repository;

import com.concessionaria.gomez.domain.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByCpfCnpj(String cpfCnpj);

    @Query("select (count(c) > 0) from Cliente c where "
            + "replace(replace(replace(replace(c.cpfCnpj, '.', ''), '-', ''), '/', ''), ' ', '') = "
            + "replace(replace(replace(replace(:documento, '.', ''), '-', ''), '/', ''), ' ', '')")
    boolean existsByCpfCnpj(@Param("documento") String cpfCnpj);

    @Query("select (count(c) > 0) from Cliente c where c.id <> :id and "
            + "replace(replace(replace(replace(c.cpfCnpj, '.', ''), '-', ''), '/', ''), ' ', '') = "
            + "replace(replace(replace(replace(:documento, '.', ''), '-', ''), '/', ''), ' ', '')")
    boolean existsByCpfCnpjAndIdNot(@Param("documento") String cpfCnpj, @Param("id") Long id);
}
