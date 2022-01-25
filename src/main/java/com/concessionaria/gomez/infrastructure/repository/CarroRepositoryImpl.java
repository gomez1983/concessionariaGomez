package com.concessionaria.gomez.infrastructure.repository;

import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

@Component
public class CarroRepositoryImpl implements CarroRepository {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public List<Carro> listar() {
        return manager.createQuery("from Carro", Carro.class)
                .getResultList();
    }

    @Override
    public Carro buscar(Long id) {
        return manager.find(Carro.class, id);
    }

    @Transactional
    @Override
    public Carro salvar(Carro cozinha) {
        return manager.merge(cozinha);
    }

    @Transactional
    @Override
    public void remover(Carro cozinha) {
        cozinha = buscar(cozinha.getId());
        manager.remove(cozinha);
    }

}