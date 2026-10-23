package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.ClienteModel;
import com.concessionaria.gomez.domain.model.Cliente;
import com.concessionaria.gomez.domain.model.Endereco;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteModelAssemblerTest {

    private ClienteModelAssembler assembler;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assembler = new ClienteModelAssembler();
        ReflectionTestUtils.setField(assembler, "modelMapper", new ModelMapper());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Deve converter Cliente ativo para ClienteModel com dados de endereço e links HATEOAS")
    void deveConverterClienteParaModelComLinksHateoas() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Maria Silva");
        cliente.setCpfCnpj("11122233344");
        cliente.setTelefone("(11) 99999-8888");
        cliente.setEmail("maria@email.com");
        cliente.setAtivo(true);
        cliente.setDataCadastro(OffsetDateTime.now());

        Endereco endereco = new Endereco();
        endereco.setCep("01001-000");
        endereco.setLogradouro("Praça da Sé");
        endereco.setNumero("100");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        cliente.setEndereco(endereco);

        ClienteModel model = assembler.toModel(cliente);

        assertThat(model).isNotNull();
        assertThat(model.getId()).isEqualTo(1L);
        assertThat(model.getNome()).isEqualTo("Maria Silva");
        assertThat(model.getCpfCnpj()).isEqualTo("11122233344");
        assertThat(model.getAtivo()).isTrue();
        assertThat(model.getEndereco()).isNotNull();
        assertThat(model.getEndereco().getCidade()).isEqualTo("São Paulo");

        // Links HATEOAS
        assertThat(model.hasLink("self")).isTrue();
        assertThat(model.getLink("self").orElseThrow().getHref()).endsWith("/clientes/1");

        assertThat(model.hasLink("clientes")).isTrue();
        assertThat(model.getLink("clientes").orElseThrow().getHref()).endsWith("/clientes");

        assertThat(model.hasLink("inativar")).isTrue();
        assertThat(model.getLink("inativar").orElseThrow().getHref()).endsWith("/clientes/1/ativo");
    }

    @Test
    @DisplayName("Deve gerar link de ativar quando cliente estiver inativo")
    void deveGerarLinkAtivarQuandoClienteInativo() {
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setNome("João Souza");
        cliente.setCpfCnpj("55566677788");
        cliente.setAtivo(false);

        ClienteModel model = assembler.toModel(cliente);

        assertThat(model).isNotNull();
        assertThat(model.getAtivo()).isFalse();
        assertThat(model.hasLink("ativar")).isTrue();
        assertThat(model.hasLink("inativar")).isFalse();
        assertThat(model.getLink("ativar").orElseThrow().getHref()).endsWith("/clientes/2/ativo");
    }

    @Test
    @DisplayName("Deve retornar null quando Cliente for nulo")
    void deveRetornarNullQuandoClienteNulo() {
        assertThat(assembler.toModel(null)).isNull();
    }

    @Test
    @DisplayName("Deve converter lista de clientes para lista de models")
    void deveConverterListaDeClientes() {
        Cliente c1 = new Cliente();
        c1.setId(1L);
        c1.setNome("Cliente 1");

        Cliente c2 = new Cliente();
        c2.setId(2L);
        c2.setNome("Cliente 2");

        List<ClienteModel> models = assembler.toCollectionModel(List.of(c1, c2));

        assertThat(models).hasSize(2);
        assertThat(models.get(0).hasLink("self")).isTrue();
        assertThat(models.get(1).hasLink("self")).isTrue();
    }
}
