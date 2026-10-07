-- ============================================================================
-- V4 — Endereços pelo vínculo user_addresses (Modelo de Dados v2).
--
-- Antes: addresses.user_id ligava cada endereço ao dono (1:N), e o restaurante
-- apontava para um desses endereços do dono.
-- Depois: o usuário se liga aos endereços por user_addresses (com rótulo e a
-- marca de padrão); o restaurante tem um endereço só dele; addresses não sabe
-- quem a referencia.
--
-- A ordem importa: os endereços que existem hoje são todos de usuários e viram
-- vínculos ANTES de o restaurante ganhar a cópia dele — a cópia não pode virar
-- endereço do usuário.
-- ============================================================================

-- 1. O vínculo. address_id é único: um endereço pertence a um vínculo só.
--    "Um padrão por usuário" é uma restrição de exclusão adiada para o commit:
--    ao trocar a lista, o Hibernate insere os novos vínculos antes de apagar os
--    antigos, e por um instante o usuário tem dois padrões. Um índice único
--    parcial (a forma imediata da mesma regra) recusaria essa troca válida.
CREATE TABLE user_addresses (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    address_id      UUID          NOT NULL UNIQUE REFERENCES addresses (id) ON DELETE RESTRICT,
    label           VARCHAR(50),
    is_default      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(100),
    last_updated_by VARCHAR(100),
    CONSTRAINT ex_user_addresses_one_default
        EXCLUDE USING btree (user_id WITH =) WHERE (is_default)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_user_addresses_user_id ON user_addresses (user_id);

-- 2. Cada endereço existente vira um vínculo do seu dono. O padrão de cada
--    usuário é o endereço de menor id: critério arbitrário, mas determinístico —
--    o banco não guardava qual era o principal.
INSERT INTO user_addresses (user_id, address_id, label, is_default, created_at, last_updated_at,
                            created_by, last_updated_by)
SELECT a.user_id,
       a.id,
       NULL,
       ROW_NUMBER() OVER (PARTITION BY a.user_id ORDER BY a.id) = 1,
       NOW(), NOW(), 'system', 'system'
FROM addresses a;

-- 3. Cada restaurante ganha a própria cópia do endereço que usava (que era do
--    dono e continua sendo). A cópia ainda precisa de user_id porque a coluna é
--    NOT NULL até o passo 4; ela não entra em user_addresses.
ALTER TABLE restaurants ADD COLUMN new_address_id UUID;
UPDATE restaurants SET new_address_id = gen_random_uuid();

INSERT INTO addresses (id, user_id, street, number, complement, neighborhood, city, state, zip_code)
SELECT r.new_address_id, a.user_id, a.street, a.number, a.complement, a.neighborhood, a.city, a.state,
       a.zip_code
FROM restaurants r
JOIN addresses a ON a.id = r.address_id;

UPDATE restaurants SET address_id = new_address_id;
ALTER TABLE restaurants DROP COLUMN new_address_id;

DROP INDEX idx_restaurants_address_id;
ALTER TABLE restaurants ADD CONSTRAINT uk_restaurants_address_id UNIQUE (address_id);

-- 4. O endereço deixa de saber quem é o dono.
DROP INDEX idx_addresses_user_id;
ALTER TABLE addresses DROP COLUMN user_id;
