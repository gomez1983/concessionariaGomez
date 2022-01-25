package com.concessionaria.gomez.jpa;

import com.concessionaria.gomez.ConcessionariaApiApplication;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import java.util.List;

public class ConsultaCarroMain {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new SpringApplicationBuilder(ConcessionariaApiApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);

        CarroRepository carroRepository = applicationContext.getBean(CarroRepository.class);

        List<Carro> carros = carroRepository.listar();

        for (Carro carro : carros){
            System.out.println(carro.getNome());
        }

    }

}