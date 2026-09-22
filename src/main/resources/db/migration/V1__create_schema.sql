-- ============================================================================
-- V1 — Schema inicial do agregado de usuário.
--
-- O schema é propriedade do Flyway: o Hibernate roda com ddl-auto: validate e
-- nunca altera nada. Toda mudança futura entra como uma nova migration.
--
-- Normalização: papéis e endereços em tabelas próprias (1FN); a única chave
-- composta (user_roles) não tem atributo dependente de parte dela (2FN);
-- users não guarda nada derivável de outra coluna não-chave (3FN); e todo
-- determinante — email, login — é chave candidata (BCNF).
-- ============================================================================

-- Catálogo de papéis. Linhas criadas só por migration: a aplicação apenas lê.
CREATE TABLE roles (
    id   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50)  NOT NULL UNIQUE
);

-- Raiz do agregado. As colunas de auditoria guardam "quando" (vindo do núcleo,
-- que recebe o instante por parâmetro) e "quem" (preenchido pelo listener do
-- Spring Data a partir do contexto de segurança).
CREATE TABLE users (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150)  NOT NULL,
    email           VARCHAR(150)  NOT NULL UNIQUE,
    login           VARCHAR(50)   NOT NULL UNIQUE,
    password        VARCHAR(100)  NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(100),
    last_updated_by VARCHAR(100)
);

-- N:M entre usuários e papéis. A chave primária composta impede o vínculo duplicado.
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Parte do agregado: endereço não existe sem dono e não é acessado por conta própria.
CREATE TABLE addresses (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    street       VARCHAR(150)  NOT NULL,
    number       VARCHAR(20)   NOT NULL,
    complement   VARCHAR(100),
    neighborhood VARCHAR(100)  NOT NULL,
    city         VARCHAR(100)  NOT NULL,
    state        VARCHAR(2)    NOT NULL,
    zip_code     VARCHAR(8)    NOT NULL
);

-- Só o hash do token é persistido; o valor em claro existe apenas no e-mail enviado.
CREATE TABLE password_reset_tokens (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(100)  NOT NULL UNIQUE,
    expires_at TIMESTAMP     NOT NULL,
    used       BOOLEAN       NOT NULL DEFAULT FALSE
);

-- A busca paginada filtra por nome sem diferenciar caixa; o índice funcional atende
-- exatamente essa expressão.
CREATE INDEX idx_users_name_lower          ON users (LOWER(name));
CREATE INDEX idx_addresses_user_id         ON addresses (user_id);
CREATE INDEX idx_reset_tokens_user_id      ON password_reset_tokens (user_id);

-- Seed do catálogo: os três papéis reconhecidos pelo domínio (RoleName).
INSERT INTO roles (name) VALUES
    ('ROLE_OWNER'),
    ('ROLE_CUSTOMER'),
    ('ROLE_ADMIN');
