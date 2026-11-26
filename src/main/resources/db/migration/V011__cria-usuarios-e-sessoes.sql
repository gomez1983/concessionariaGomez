-- ==========================================================
-- Etapa 3a: usuários do painel e sessões de renovação (refresh token)
-- Somente cria tabelas novas; nenhuma tabela existente é alterada.
-- Os usuários iniciais são criados pela aplicação a partir do .env (sem senha no repositório).
-- ==========================================================

create table usuario (
    id bigint not null auto_increment,
    nome varchar(100) not null,
    email varchar(100) not null,
    senha_hash varchar(100) not null,
    perfil varchar(20) not null,
    ativo tinyint(1) not null default 1,
    tentativas_falhas int not null default 0,
    bloqueado_ate datetime null,
    ultimo_login datetime null,
    data_cadastro datetime not null,
    data_atualizacao datetime not null,

    primary key (id),
    unique key uk_usuario_email (email)
) engine=InnoDB default charset=utf8mb4;

create table sessao_usuario (
    id bigint not null auto_increment,
    usuario_id bigint not null,
    token_hash char(64) not null,
    criada_em datetime not null,
    expira_em datetime not null,
    revogada_em datetime null,
    motivo_revogacao varchar(20) null,
    substituida_por_id bigint null,

    primary key (id),
    unique key uk_sessao_token_hash (token_hash),
    index idx_sessao_usuario (usuario_id, revogada_em),
    constraint fk_sessao_usuario foreign key (usuario_id) references usuario (id),
    constraint fk_sessao_substituta foreign key (substituida_por_id) references sessao_usuario (id)
) engine=InnoDB default charset=utf8mb4;
