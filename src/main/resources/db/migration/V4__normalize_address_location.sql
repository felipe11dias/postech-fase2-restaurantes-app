-- ============================================================================
-- V4 — Normalização dos catálogos geográficos e do endereço.
--
-- Converte cidade/UF armazenadas em texto em chaves estrangeiras, preservando
-- os endereços já gravados pelas migrations anteriores.
-- ============================================================================

CREATE TABLE states (
    id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name    VARCHAR(100) NOT NULL,
    acronym VARCHAR(2)   NOT NULL UNIQUE
);

CREATE TABLE cities (
    id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    state_id UUID         NOT NULL REFERENCES states (id) ON DELETE RESTRICT,
    name     VARCHAR(100) NOT NULL,
    CONSTRAINT uk_cities_state_name UNIQUE (state_id, name)
);

INSERT INTO states (name, acronym)
SELECT CASE UPPER(TRIM(state))
           WHEN 'AC' THEN 'Acre'
           WHEN 'AL' THEN 'Alagoas'
           WHEN 'AP' THEN 'Amapá'
           WHEN 'AM' THEN 'Amazonas'
           WHEN 'BA' THEN 'Bahia'
           WHEN 'CE' THEN 'Ceará'
           WHEN 'DF' THEN 'Distrito Federal'
           WHEN 'ES' THEN 'Espírito Santo'
           WHEN 'GO' THEN 'Goiás'
           WHEN 'MA' THEN 'Maranhão'
           WHEN 'MT' THEN 'Mato Grosso'
           WHEN 'MS' THEN 'Mato Grosso do Sul'
           WHEN 'MG' THEN 'Minas Gerais'
           WHEN 'PA' THEN 'Pará'
           WHEN 'PB' THEN 'Paraíba'
           WHEN 'PR' THEN 'Paraná'
           WHEN 'PE' THEN 'Pernambuco'
           WHEN 'PI' THEN 'Piauí'
           WHEN 'RJ' THEN 'Rio de Janeiro'
           WHEN 'RN' THEN 'Rio Grande do Norte'
           WHEN 'RS' THEN 'Rio Grande do Sul'
           WHEN 'RO' THEN 'Rondônia'
           WHEN 'RR' THEN 'Roraima'
           WHEN 'SC' THEN 'Santa Catarina'
           WHEN 'SP' THEN 'São Paulo'
           WHEN 'SE' THEN 'Sergipe'
           WHEN 'TO' THEN 'Tocantins'
           ELSE UPPER(TRIM(state))
       END,
       UPPER(TRIM(state))
FROM addresses
GROUP BY UPPER(TRIM(state));

INSERT INTO cities (id, state_id, name)
SELECT CASE WHEN states.acronym = 'SP' AND LOWER(TRIM(addresses.city)) = 'são paulo'
            THEN 'c0000000-0000-4000-8000-000000000001'::UUID
            ELSE gen_random_uuid()
       END,
       states.id,
       MIN(TRIM(addresses.city))
FROM addresses
JOIN states ON states.acronym = UPPER(TRIM(addresses.state))
GROUP BY states.id, LOWER(TRIM(addresses.city));

ALTER TABLE addresses ADD COLUMN city_id UUID;

UPDATE addresses
SET city_id = cities.id
FROM cities
JOIN states ON states.id = cities.state_id
WHERE LOWER(cities.name) = LOWER(TRIM(addresses.city))
  AND states.acronym = UPPER(TRIM(addresses.state));

ALTER TABLE addresses
    ALTER COLUMN city_id SET NOT NULL,
    ADD CONSTRAINT fk_addresses_city FOREIGN KEY (city_id) REFERENCES cities (id) ON DELETE RESTRICT;

CREATE INDEX idx_addresses_city_id ON addresses (city_id);

ALTER TABLE addresses DROP COLUMN city, DROP COLUMN state;
