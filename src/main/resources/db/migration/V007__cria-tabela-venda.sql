create table venda (
    id bigint not null auto_increment,
    carro_id bigint not null,
    cliente_id bigint not null,
    data_venda datetime not null,
    valor_venda decimal(10,2) not null,
    observacoes varchar(255),

    primary key (id),
    constraint fk_venda_carro foreign key (carro_id) references carro (id),
    constraint fk_venda_cliente foreign key (cliente_id) references cliente (id)
) engine=InnoDB default charset=utf8mb4;
