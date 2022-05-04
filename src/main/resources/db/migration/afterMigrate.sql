set foreign_key_checks = 0;

delete from carro;

set foreign_key_checks = 1;

alter table carro auto_increment = 1;

insert into carro (id, marca, modelo, ano, compra, dataCompra) values (1, 'Chevrolet', 'Chevette', 1987, 15000,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (2, 'Ford', 'Fiesta', 2007, 21000,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (3, 'Chevrolet', 'Cruze', 2014, 55000,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (4, 'Chevrolet', 'Tracker', 2015, 58990,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (5, 'Ford', 'EcoSport', 2014, 54990,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (6, 'Ford', 'F-250', 2004, 100000,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (7, 'Ford', 'Fusion', 2018, 108900,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (8, 'Volkswagen', 'Passat', 2018, 169900,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (9, 'Volkswagen', 'Tiguan', 2021, 255000,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (10, 'Volkswagen', 'Polo', 2019, 55900,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (11, 'Volkswagen', 'Jetta', 2019, 112900,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (12, 'Mitsubishi', 'Pajero TR4', 2014, 59900,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (13, 'Mitsubishi', 'Eclipse Cross', 2020, 189990,  utc_timestamp);
insert into carro (id, marca, modelo, ano, compra, dataCompra) values (14, 'Mitsubishi', 'ASX', 2018, 86990,  utc_timestamp);