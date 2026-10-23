create table foto_galeria_carro (
    id bigint not null auto_increment,
    carro_id bigint not null,
    nome_arquivo varchar(150) not null,
    descricao varchar(150) not null,
    content_type varchar(80) not null,
    tamanho bigint not null,
    primary key (id),
    index idx_galeria_carro (carro_id),
    constraint fk_galeria_carro foreign key (carro_id) references carro (id)
) engine=InnoDB default charset=utf8mb4;
