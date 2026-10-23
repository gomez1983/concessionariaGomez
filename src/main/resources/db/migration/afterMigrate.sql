set foreign_key_checks = 0;

delete from foto_carro;
delete from venda;
delete from carro;
delete from cliente;

set foreign_key_checks = 1;

alter table venda auto_increment = 1;
alter table carro auto_increment = 1;
alter table cliente auto_increment = 1;

insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (1, 'Chevrolet', 'Chevette', 1987, 15000,  utc_timestamp, true, 185000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (2, 'Ford', 'Fiesta', 2007, 21000,  utc_timestamp, true, 120000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (3, 'Chevrolet', 'Cruze', 2014, 55000,  utc_timestamp, true, 78000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (4, 'Chevrolet', 'Tracker', 2015, 58990,  utc_timestamp, true, 65000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (5, 'Ford', 'EcoSport', 2014, 54990,  utc_timestamp, true, 92000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (6, 'Ford', 'F-250', 2004, 100000,  utc_timestamp, true, 210000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (7, 'Ford', 'Fusion', 2018, 108900,  utc_timestamp, true, 45000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (8, 'Volkswagen', 'Passat', 2018, 169900,  utc_timestamp, true, 38000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (9, 'Volkswagen', 'Tiguan', 2021, 255000,  utc_timestamp, true, 18000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (10, 'Volkswagen', 'Polo', 2019, 55900,  utc_timestamp, true, 42000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (11, 'Volkswagen', 'Jetta', 2019, 112900,  utc_timestamp, true, 35000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (12, 'Mitsubishi', 'Pajero TR4', 2014, 59900,  utc_timestamp, true, 115000);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (13, 'Mitsubishi', 'Eclipse Cross', 2020, 189990,  utc_timestamp, true, 0);
insert into carro (id, marca, modelo, ano, compra, dataCompra, ativo, quilometragem) values (14, 'Mitsubishi', 'ASX', 2018, 86990,  utc_timestamp, true, 52000);

insert into cliente (id, nome, cpf_cnpj, telefone, email, ativo, data_cadastro, endereco_cep, endereco_logradouro, endereco_numero, endereco_complemento, endereco_bairro, endereco_cidade, endereco_estado)
values (1, 'Carlos Eduardo Silva', '12345678901', '(11) 98765-4321', 'carlos.silva@email.com', true, utc_timestamp, '01310-100', 'Avenida Paulista', '1000', 'Apto 42', 'Bela Vista', 'São Paulo', 'SP');

insert into cliente (id, nome, cpf_cnpj, telefone, email, ativo, data_cadastro, endereco_cep, endereco_logradouro, endereco_numero, endereco_complemento, endereco_bairro, endereco_cidade, endereco_estado)
values (2, 'Mariana Costa Ferreira', '98765432100', '(21) 99876-5432', 'mariana.costa@email.com', true, utc_timestamp, '22041-001', 'Avenida Atlântica', '500', 'Bloco B', 'Copacabana', 'Rio de Janeiro', 'RJ');

insert into cliente (id, nome, cpf_cnpj, telefone, email, ativo, data_cadastro, endereco_cep, endereco_logradouro, endereco_numero, endereco_complemento, endereco_bairro, endereco_cidade, endereco_estado)
values (3, 'Auto Locadora Gomez Ltda', '12345678000199', '(11) 3214-5500', 'contato@locadoragomez.com.br', true, utc_timestamp, '04578-000', 'Avenida das Nações Unidas', '12551', 'Conjunto 14', 'Brooklin', 'São Paulo', 'SP');
