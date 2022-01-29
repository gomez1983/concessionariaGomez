create table carro (
	id bigint not null auto_increment,
    marca varchar(60) not null,
    modelo varchar(60) not null,
    ano bigint not null,
    compra double not null,

    primary key(id)
)engine=InnoDB default charset=utf8;