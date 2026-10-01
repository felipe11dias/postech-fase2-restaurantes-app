-- ============================================================================
-- V3 — Schema do agregado de restaurante.
--
-- Tabela de restaurantes vinculando o dono (users) e o endereço (addresses).
-- ============================================================================

CREATE TABLE restaurants (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    address_id        UUID          NOT NULL REFERENCES addresses (id) ON DELETE RESTRICT,
    name              VARCHAR(150)  NOT NULL,
    office_hour_start TIME          NOT NULL,
    office_hour_end   TIME          NOT NULL,
    created_at        TIMESTAMP     NOT NULL,
    last_updated_at   TIMESTAMP     NOT NULL,
    created_by        VARCHAR(100),
    last_updated_by   VARCHAR(100)
);

CREATE INDEX idx_restaurants_user_id     ON restaurants (user_id);
CREATE INDEX idx_restaurants_address_id  ON restaurants (address_id);
CREATE INDEX idx_restaurants_name_lower  ON restaurants (LOWER(name));
