package com.concessionaria.gomez.jpa;

import com.concessionaria.gomez.ConcessionariaApiApplication;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

public class InclusaoCarroMain {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new SpringApplicationBuilder(ConcessionariaApiApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);

        CarroRepository carroRepository = applicationContext.getBean(CarroRepository.class);

        Carro carro1 = new Carro();
        carro1.setNome("Ford Fiesta");

        Carro carro2 = new Carro();
        carro2.setNome("Mitsubish ASX");

        carro1 = carroRepository.salvar(carro1);
        carro2 = carroRepository.salvar(carro2);

        System.out.printf("%d - %s\n", carro1.getId(), carro1.getNome());
        System.out.printf("%d - %s\n", carro2.getId(), carro2.getNome());

    }
}