alter table carro add ativo tinyint(1) not null;
update carro set ativo = true;