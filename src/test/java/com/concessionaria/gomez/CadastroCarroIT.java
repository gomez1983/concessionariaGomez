package com.concessionaria.gomez;

import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import static org.hamcrest.CoreMatchers.equalTo;

import com.concessionaria.gomez.util.ResourceUtils;
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

    private static final int CARRO_ID_INEXISTENTE = 100;

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private CarroRepository carroRepository;

    private Carro carroMercedes;
    private int quantidadeCarrosCadastrados;
    private String jsonCorretoCarroAudi;

    @BeforeEach
    public void setUp(){
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.port = port;
        RestAssured.basePath = "/carros";

        jsonCorretoCarroAudi = ResourceUtils.getContentFromResource(
                "/json/correto/carro-audi.json");

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
    public void deveRetornarQuantidadeCorretaDeCarros_QuandoConsultarCarros() { /*Método para validação do corpo de resposta*/
        RestAssured.given()
            .accept(ContentType.JSON)
        .when()
            .get()
        .then()
            .body("", Matchers.hasSize(quantidadeCarrosCadastrados));
            /*.body("modelo", Matchers.hasItems("Fiesta", "ASX")); ---  Exemplos com outros filtros*/
    }

    @Test
    public void deveRetornarStatus201_QuandoCadastrarCarro() {
        RestAssured.given()
            .body(jsonCorretoCarroAudi)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
        .when()
            .post()
        .then()
            .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    public void deveRetornarRespostasEStatusCorretos_QuandoConsultarCarroExistente(){
        RestAssured.given()
            .pathParam("carroId", carroMercedes.getId())
            .accept(ContentType.JSON)
        .when()
            .get("/{carroId}")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("modelo", equalTo(carroMercedes.getModelo()));
    }

    @Test
    public void deveRetorbarStatus404_QuandoConsultarcarroInexistente(){
        RestAssured.given()
            .pathParam("carroId", CARRO_ID_INEXISTENTE)
            .accept(ContentType.JSON)
        .when()
            .get("/{carroId}")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    /*------------- Método para inserir massas de dados para os testes rodarem ----------*/

    private void prepararDados(){
        carroMercedes = new Carro();
        carroMercedes.setMarca("Mercedes");
        carroMercedes.setModelo("Classe A");
        carroMercedes.setAno(2022);
        carroMercedes.setCompra(319900.00);
        carroRepository.save(carroMercedes);

        Carro carroBMW = new Carro();
        carroBMW.setMarca("BMW");
        carroBMW.setModelo("M5");
        carroBMW.setAno(2018);
        carroBMW.setCompra(150000.00);
        carroRepository.save(carroBMW);

        quantidadeCarrosCadastrados = (int) carroRepository.count();
    }
}