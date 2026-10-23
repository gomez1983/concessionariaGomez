create table cliente (
    id bigint not null auto_increment,
    nome varchar(80) not null,
    cpf_cnpj varchar(20) not null,
    telefone varchar(20),
    email varchar(100),
    ativo tinyint(1) not null default 1,
    data_cadastro datetime not null,
    endereco_cep varchar(9),
    endereco_logradouro varchar(100),
    endereco_numero varchar(20),
    endereco_complemento varchar(60),
    endereco_bairro varchar(60),
    endereco_cidade varchar(60),
    endereco_estado varchar(2),

    primary key (id),
    constraint uk_cliente_cpf_cnpj unique (cpf_cnpj)
) engine=InnoDB default charset=utf8mb4;
