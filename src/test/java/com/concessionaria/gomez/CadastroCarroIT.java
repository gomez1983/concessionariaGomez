package com.concessionaria.gomez;

import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import com.concessionaria.gomez.util.DatabaseCleaner;
import com.concessionaria.gomez.util.ResourceUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;

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
    private Carro carroBMW;
    private int quantidadeCarrosCadastrados;
    private String jsonCorretoCarroAudi;
    private String jsonCorretoCarroMercedesAtualizado;
    private String jsonIncorretoCarro;

    @BeforeEach
    public void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.port = port;
        RestAssured.basePath = "/carros";

        jsonCorretoCarroAudi = ResourceUtils.getContentFromResource(
                "/json/correto/carro-audi.json");
        jsonCorretoCarroMercedesAtualizado = ResourceUtils.getContentFromResource(
                "/json/correto/carro-mercedes-atualizado.json");
        jsonIncorretoCarro = ResourceUtils.getContentFromResource(
                "/json/incorreto/carro-invalido.json");

        databaseCleaner.clearTables();
        prepararDados();
    }

    @Test
    @DisplayName("Deve retornar status 200 quando consultar todos os carros")
    public void deveRetornarStatus200_QuandoConsultarCarros() {
        RestAssured.given()
                .accept(ContentType.JSON)
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Deve retornar a quantidade correta de carros cadastrados")
    public void deveRetornarQuantidadeCorretaDeCarros_QuandoConsultarCarros() {
        RestAssured.given()
                .accept(ContentType.JSON)
            .when()
                .get()
            .then()
                .body("", Matchers.hasSize(quantidadeCarrosCadastrados));
    }

    @Test
    @DisplayName("Deve retornar apenas carros do ano especificado quando consultar com filtro de ano")
    public void deveRetornarCarrosFiltradosPorAno_QuandoConsultarComParametroAno() {
        RestAssured.given()
                .queryParam("ano", 2022)
                .accept(ContentType.JSON)
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("", Matchers.hasSize(1))
                .body("[0].modelo", equalTo("Classe A"))
                .body("[0].ano", equalTo(2022));
    }

    @Test
    @DisplayName("Deve retornar status 200 e dados corretos quando consultar carro por ID existente")
    public void deveRetornarRespostasEStatusCorretos_QuandoConsultarCarroExistente() {
        RestAssured.given()
                .pathParam("carroId", carroMercedes.getId())
                .accept(ContentType.JSON)
            .when()
                .get("/{carroId}")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(carroMercedes.getId().intValue()))
                .body("marca", equalTo(carroMercedes.getMarca()))
                .body("modelo", equalTo(carroMercedes.getModelo()))
                .body("ano", equalTo(carroMercedes.getAno()))
                .body("compra", equalTo(319900.00f))
                .body("ativo", equalTo(true));
    }

    @Test
    @DisplayName("Deve calcular e retornar o preço de venda com a margem de 10% no endpoint de consulta")
    public void deveRetornarPrecoVendaSugeridoComMargem10PorCento_QuandoConsultarCarroExistente() {
        // Compra: 319900.00 -> Venda com 10%: 351890.00
        RestAssured.given()
                .pathParam("carroId", carroMercedes.getId())
                .accept(ContentType.JSON)
            .when()
                .get("/{carroId}")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("venda", equalTo(351890.00f));
    }

    @Test
    @DisplayName("Deve retornar status 404 quando consultar carro com ID inexistente")
    public void deveRetornarStatus404_QuandoConsultarcarroInexistente() {
        RestAssured.given()
                .pathParam("carroId", CARRO_ID_INEXISTENTE)
                .accept(ContentType.JSON)
            .when()
                .get("/{carroId}")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Deve retornar status 201 e preço de venda sugerido ao cadastrar carro com dados válidos")
    public void deveRetornarStatus201_QuandoCadastrarCarro() {
        // Audi compra 210000.00 -> venda sugerida 231000.00
        RestAssured.given()
                .body(jsonCorretoCarroAudi)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
            .when()
                .post()
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("marca", equalTo("Audi"))
                .body("modelo", equalTo("A8"))
                .body("ano", equalTo(2022))
                .body("compra", equalTo(210000.00f))
                .body("venda", equalTo(231000.00f));
    }

    @Test
    @DisplayName("Deve retornar status 400 Bad Request ao tentar cadastrar carro com dados inválidos")
    public void deveRetornarStatus400_QuandoCadastrarCarroComDadosInvalidos() {
        RestAssured.given()
                .body(jsonIncorretoCarro)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
            .when()
                .post()
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Deve retornar status 200 e dados atualizados ao atualizar carro existente via PUT")
    public void deveRetornarStatus200EObjetoAtualizado_QuandoAtualizarCarroExistente() {
        RestAssured.given()
                .pathParam("carroId", carroMercedes.getId())
                .body(jsonCorretoCarroMercedesAtualizado)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
            .when()
                .put("/{carroId}")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("marca", equalTo("Mercedes-Benz"))
                .body("modelo", equalTo("Classe A Sedan"))
                .body("ano", equalTo(2023))
                .body("compra", equalTo(340000.00f))
                .body("venda", equalTo(374000.00f));
    }

    @Test
    @DisplayName("Deve retornar status 204 ao inativar carro e atualizar status ativo para false")
    public void deveRetornarStatus204_QuandoInativarCarro() {
        RestAssured.given()
                .pathParam("carroId", carroMercedes.getId())
                .accept(ContentType.JSON)
            .when()
                .delete("/{carroId}/ativo")
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        RestAssured.given()
                .pathParam("carroId", carroMercedes.getId())
                .accept(ContentType.JSON)
            .when()
                .get("/{carroId}")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("ativo", equalTo(false));
    }

    @Test
    @DisplayName("Deve retornar status 204 ao ativar carro e atualizar status ativo para true")
    public void deveRetornarStatus204_QuandoAtivarCarro() {
        // Primeiro inativa no banco
        carroMercedes.inativar();
        carroRepository.save(carroMercedes);

        RestAssured.given()
                .pathParam("carroId", carroMercedes.getId())
                .accept(ContentType.JSON)
            .when()
                .put("/{carroId}/ativo")
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        RestAssured.given()
                .pathParam("carroId", carroMercedes.getId())
                .accept(ContentType.JSON)
            .when()
                .get("/{carroId}")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("ativo", equalTo(true));
    }

    @Test
    @DisplayName("Deve retornar status 204 ao excluir carro existente e 404 ao buscá-lo em seguida")
    public void deveRetornarStatus204_QuandoExcluirCarroExistente() {
        RestAssured.given()
                .pathParam("carroId", carroBMW.getId())
                .accept(ContentType.JSON)
            .when()
                .delete("/{carroId}")
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        RestAssured.given()
                .pathParam("carroId", carroBMW.getId())
                .accept(ContentType.JSON)
            .when()
                .get("/{carroId}")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Deve retornar status 404 ao tentar excluir carro com ID inexistente")
    public void deveRetornarStatus404_QuandoExcluirCarroInexistente() {
        RestAssured.given()
                .pathParam("carroId", CARRO_ID_INEXISTENTE)
                .accept(ContentType.JSON)
            .when()
                .delete("/{carroId}")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    /*------------- Metodo para inserir massas de dados para os testes rodarem ----------*/

    private void prepararDados() {
        carroMercedes = new Carro();
        carroMercedes.setMarca("Mercedes");
        carroMercedes.setModelo("Classe A");
        carroMercedes.setAno(2022);
        carroMercedes.setCompra(new BigDecimal("319900.00"));
        carroMercedes.setAtivo(true);
        carroRepository.save(carroMercedes);

        carroBMW = new Carro();
        carroBMW.setMarca("BMW");
        carroBMW.setModelo("M5");
        carroBMW.setAno(2018);
        carroBMW.setCompra(new BigDecimal("150000.00"));
        carroBMW.setAtivo(true);
        carroRepository.save(carroBMW);

        quantidadeCarrosCadastrados = (int) carroRepository.count();
    }
}
