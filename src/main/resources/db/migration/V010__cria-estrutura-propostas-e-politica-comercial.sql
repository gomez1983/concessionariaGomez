-- ==========================================================
-- Fase 7 (Etapa 7.1): Política Comercial, Motor Financeiro
-- e Modelo de Dados de Propostas Comerciais e Revisões
-- ==========================================================

alter table carro
    add column preco_anunciado decimal(12,2) null,
    add column disponibilidade_comercial varchar(20) not null default 'DISPONIVEL',
    add column reserva_valida_ate datetime null,
    add column reserva_responsavel varchar(100) null;

update carro set disponibilidade_comercial = 'VENDIDO' where ativo = 0;

create table politica_comercial (
    id bigint not null auto_increment,
    contexto varchar(30) not null,
    carro_id bigint null,
    versao int not null default 1,
    margem_desejada decimal(10,6) not null,
    margem_autonomia decimal(10,6) not null,
    margem_piso_absoluto decimal(10,6) not null,
    desconto_maximo_autonomia_reais decimal(12,2) null,
    desconto_maximo_autonomia_percentual decimal(10,6) null,
    responsavel_cadastro varchar(100) not null,
    vigente_a_partir_de datetime not null,
    vigente_ate datetime null,
    ativa tinyint(1) not null default 1,
    data_cadastro datetime not null,

    primary key (id),
    index idx_politica_contexto_vigencia (contexto, ativa, vigente_a_partir_de),
    index idx_politica_carro (carro_id),
    constraint fk_politica_carro foreign key (carro_id) references carro (id)
) engine=InnoDB default charset=utf8mb4;

insert into politica_comercial (
    contexto, carro_id, versao,
    margem_desejada, margem_autonomia, margem_piso_absoluto,
    desconto_maximo_autonomia_reais, desconto_maximo_autonomia_percentual,
    responsavel_cadastro, vigente_a_partir_de, ativa, data_cadastro
) values (
    'LOJA_PADRAO', null, 1,
    0.100000, 0.070000, 0.050000,
    5000.00, 0.050000,
    'sistema@concessionariagomez.com.br', '2020-01-01 00:00:00', 1, utc_timestamp
);

create table custo_veiculo (
    id bigint not null auto_increment,
    carro_id bigint not null,
    natureza varchar(45) not null,
    base_calculo varchar(35) not null,
    valor decimal(12,6) null,
    confirmado tinyint(1) not null default 0,
    descricao varchar(150) not null,
    responsavel varchar(100) null,
    data_registro datetime not null,

    primary key (id),
    index idx_custo_veiculo_carro (carro_id),
    constraint fk_custo_veiculo_carro foreign key (carro_id) references carro (id)
) engine=InnoDB default charset=utf8mb4;

create table campanha_comercial (
    id bigint not null auto_increment,
    codigo varchar(40) not null,
    nome varchar(120) not null,
    origem varchar(40) not null,
    valor_beneficio decimal(12,2) not null,
    reembolsavel_montadora tinyint(1) not null default 0,
    ja_refletida_no_preco_ou_custo tinyint(1) not null default 0,
    cumulativa tinyint(1) not null default 0,
    vigente_a_partir_de datetime not null,
    vigente_ate datetime null,
    ativa tinyint(1) not null default 1,
    evidencia_condicoes varchar(255) null,

    primary key (id),
    constraint uk_campanha_codigo unique (codigo)
) engine=InnoDB default charset=utf8mb4;

create table proposta_comercial (
    id bigint not null auto_increment,
    numero varchar(30) not null,
    carro_id bigint not null,
    cliente_id bigint not null,
    vendedor_identificador varchar(100) not null,
    status varchar(30) not null,
    revisao_atual_sequencia int null default 1,
    valida_ate datetime null,
    versao_concorrencia bigint not null default 0,
    data_criacao datetime not null,

    primary key (id),
    constraint uk_proposta_numero unique (numero),
    index idx_proposta_status_vendedor (status, vendedor_identificador),
    index idx_proposta_valida_ate (valida_ate),
    index idx_proposta_carro (carro_id),
    constraint fk_proposta_carro foreign key (carro_id) references carro (id),
    constraint fk_proposta_cliente foreign key (cliente_id) references cliente (id)
) engine=InnoDB default charset=utf8mb4;

create table revisao_proposta (
    id bigint not null auto_increment,
    proposta_id bigint not null,
    sequencia int not null,
    politica_comercial_id bigint not null,
    custo_aquisicao_snapshot decimal(12,2) not null,
    preco_anunciado_snapshot decimal(12,2) not null,
    preco_sugerido_snapshot decimal(12,2) not null,
    preco_negociado decimal(12,2) not null,
    custos_fixos_operacao decimal(12,2) not null default 0.00,
    soma_taxas_proporcionais decimal(10,6) not null default 0.000000,
    receitas_adicionais_elegiveis decimal(12,2) not null default 0.00,
    contribuicao_estimada decimal(12,2) not null,
    margem_contribuicao_estimada decimal(10,6) not null,
    desconto_reais decimal(12,2) not null,
    desconto_percentual decimal(10,6) not null,
    markup_aquisicao decimal(10,6) not null,
    piso_autonomia_calculado decimal(12,2) not null,
    piso_absoluto_calculado decimal(12,2) not null,
    alcada_analise varchar(40) not null,
    possui_custos_pendentes tinyint(1) not null default 0,
    submetida tinyint(1) not null default 0,
    justificativa_vendedor varchar(255) null,
    data_criacao datetime not null,

    primary key (id),
    constraint uk_revisao_proposta_seq unique (proposta_id, sequencia),
    constraint fk_revisao_proposta foreign key (proposta_id) references proposta_comercial (id),
    constraint fk_revisao_politica foreign key (politica_comercial_id) references politica_comercial (id)
) engine=InnoDB default charset=utf8mb4;

create table item_financeiro_revisao (
    id bigint not null auto_increment,
    revisao_id bigint not null,
    natureza varchar(45) not null,
    base_calculo varchar(35) not null,
    valor decimal(12,6) null,
    confirmado tinyint(1) not null default 0,
    descricao varchar(150) not null,
    origem varchar(80) null,

    primary key (id),
    index idx_item_fin_revisao (revisao_id),
    constraint fk_item_fin_revisao foreign key (revisao_id) references revisao_proposta (id)
) engine=InnoDB default charset=utf8mb4;

create table pagamento_proposta (
    id bigint not null auto_increment,
    revisao_id bigint not null,
    modalidade varchar(35) not null,
    valor decimal(12,2) not null,
    observacao varchar(150) null,

    primary key (id),
    index idx_pagamento_revisao (revisao_id),
    constraint fk_pagamento_revisao foreign key (revisao_id) references revisao_proposta (id)
) engine=InnoDB default charset=utf8mb4;

create table financiamento_proposta (
    id bigint not null auto_increment,
    revisao_id bigint not null,
    instituicao_financeira varchar(100) not null,
    principal_financiado decimal(12,2) not null,
    entrada_monetaria_relacionada decimal(12,2) not null default 0.00,
    prazo_meses int not null,
    taxa_mensal decimal(10,6) null,
    cet_anual_informado decimal(10,6) null,
    valor_parcela decimal(12,2) null,
    apenas_estimativa tinyint(1) not null default 1,
    situacao_externa varchar(40) not null default 'SIMULADO',
    valido_ate datetime null,

    primary key (id),
    constraint uk_financiamento_revisao unique (revisao_id),
    constraint fk_financiamento_revisao foreign key (revisao_id) references revisao_proposta (id)
) engine=InnoDB default charset=utf8mb4;

create table avaliacao_troca (
    id bigint not null auto_increment,
    revisao_id bigint not null,
    identificacao_veiculo_usado varchar(150) not null,
    valor_avaliacao_tecnica decimal(12,2) not null,
    valor_comercial_aceito decimal(12,2) not null,
    saldo_quitacao_verificado decimal(12,2) not null default 0.00,
    credito_liquido decimal(12,2) not null default 0.00,
    possui_pendencia_documental tinyint(1) not null default 0,
    descricao_pendencias varchar(255) null,
    valida_ate datetime null,

    primary key (id),
    constraint uk_avaliacao_troca_revisao unique (revisao_id),
    constraint fk_avaliacao_troca_revisao foreign key (revisao_id) references revisao_proposta (id)
) engine=InnoDB default charset=utf8mb4;

create table decisao_aprovacao (
    id bigint not null auto_increment,
    revisao_id bigint not null,
    decisor_autenticado varchar(100) not null,
    resultado varchar(40) not null,
    alcada varchar(40) not null,
    motivo varchar(255) null,
    data_decisao datetime not null,

    primary key (id),
    index idx_decisao_revisao (revisao_id),
    constraint fk_decisao_revisao foreign key (revisao_id) references revisao_proposta (id)
) engine=InnoDB default charset=utf8mb4;

alter table venda
    add column revisao_convertida_id bigint null,
    add column origem_legada tinyint(1) not null default 1,
    add column cancelada tinyint(1) not null default 0,
    add column motivo_cancelamento varchar(255) null,
    add column responsavel_cancelamento varchar(100) null,
    add column data_cancelamento datetime null,
    add constraint uk_venda_revisao_convertida unique (revisao_convertida_id),
    add constraint fk_venda_revisao_convertida foreign key (revisao_convertida_id) references revisao_proposta (id);
