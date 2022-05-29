package com.concessionaria.gomez;

import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.concessionaria.gomez.util.DatabaseCleaner;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("/application-test.properties")
public class CadastroCarroIT {

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private CarroRepository carroRepository;

    @BeforeEach
    public void setUp(){
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.port = port;
        RestAssured.basePath = "/carros";

        databaseCleaner.clearTables();
        prepararDados();
    }

    @Test
    public void deveRetornarStatus200_QuandoConsultarCarros() {
        RestAssured.given()
                .accept(ContentType.JSON)
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void deveConter2Carros_QuandoConsultarCarros() { /*Método para validação do corpo de resposta*/
        RestAssured.given()
            .accept(ContentType.JSON)
        .when()
            .get()
        .then()
            .body("", Matchers.hasSize(2));
            /*.body("modelo", Matchers.hasItems("Fiesta", "ASX")); ---  Exemplos com outros filtros*/
    }

    @Test
    public void deveRetornarStatus201_QuandoCadastrarCarro() {
        RestAssured.given()
                .body("{\"marca\": \"Ford\", \"modelo\" : \"Escort\", \"compra\" : \"24300\", \"ano\" : \"1997\"}")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
            .when()
                .post()
            .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    /*------------- Método para inserir massas de dados para os testes rodarem ----------*/

    private void prepararDados(){
        Carro carro1 = new Carro();
        carro1.setMarca("Honda");
        carro1.setModelo("Civic");
        carro1.setAno(2018);
        carro1.setCompra(98000.00);
        carroRepository.save(carro1);

        Carro carro2 = new Carro();
        carro2.setMarca("Audi");
        carro2.setModelo("A4");
        carro2.setAno(2022);
        carro2.setCompra(15000.00);
        carroRepository.save(carro2);
    }
}