create table foto_carro (
    carro_id bigint not null,
    nome_arquivo varchar(150) not null,
    descricao varchar(150),
    content_type varchar(80) not null,
    tamanho bigint not null,

    primary key (carro_id),
    constraint fk_foto_carro_carro foreign key (carro_id) references carro (id)
) engine=InnoDB default charset=utf8mb4;
