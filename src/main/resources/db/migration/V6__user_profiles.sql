-- ============================================================================
-- V6 — Perfis do usuário no lugar do catálogo de papéis (Modelo de Dados v2).
--
-- Antes: o tipo do usuário era um papel escolhido num catálogo (roles) e ligado
-- por user_roles. Depois: o tipo é a especialização — owners, clients,
-- couriers, admins, cada uma com a chave primária do próprio usuário — e o papel
-- de autorização é derivado do perfil pela aplicação, sem ser gravado.
--
-- Especialização sobreposta (um usuário pode ter vários perfis) e total (todo
-- usuário tem ao menos um): a parte "total" é conferida no passo 4.
-- ============================================================================

-- 1. Os enums do modelo para o entregador.
CREATE TYPE courier_vehicle_type AS ENUM ('ON_FOOT', 'BICYCLE', 'MOTORCYCLE', 'CAR');
CREATE TYPE courier_status AS ENUM ('OFFLINE', 'AVAILABLE', 'BUSY');

-- 2. As especializações. A chave primária é a chave estrangeira para users:
--    apagar o usuário apaga o perfil.
CREATE TABLE owners (
    id              UUID          PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    cnpj            VARCHAR(14)   NOT NULL UNIQUE,
    legal_name      VARCHAR(150)  NOT NULL,
    business_phone  VARCHAR(20)   NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(100),
    last_updated_by VARCHAR(100)
);

CREATE TABLE clients (
    id              UUID          PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    cpf             VARCHAR(11)   NOT NULL UNIQUE,
    phone           VARCHAR(20)   NOT NULL,
    birth_date      DATE,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(100),
    last_updated_by VARCHAR(100)
);

-- CNH e placa andam com o veículo (o COMMENT do modelo, aqui como restrição):
-- motorizado exige as duas; a pé ou de bicicleta, nenhuma.
CREATE TABLE couriers (
    id                    UUID                  PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    cpf                   VARCHAR(11)           NOT NULL UNIQUE,
    phone                 VARCHAR(20)           NOT NULL,
    driver_license_number VARCHAR(11)           UNIQUE,
    vehicle_type          courier_vehicle_type  NOT NULL,
    vehicle_plate         VARCHAR(8),
    status                courier_status        NOT NULL DEFAULT 'OFFLINE',
    created_at            TIMESTAMP             NOT NULL,
    last_updated_at       TIMESTAMP             NOT NULL,
    created_by            VARCHAR(100),
    last_updated_by       VARCHAR(100),
    CONSTRAINT ck_couriers_license_by_vehicle CHECK (
        CASE WHEN vehicle_type IN ('MOTORCYCLE', 'CAR')
             THEN driver_license_number IS NOT NULL AND vehicle_plate IS NOT NULL
             ELSE driver_license_number IS NULL AND vehicle_plate IS NULL
        END)
);

COMMENT ON COLUMN couriers.driver_license_number IS 'Obrigatório apenas para MOTORCYCLE e CAR';
COMMENT ON COLUMN couriers.vehicle_plate IS 'Obrigatório apenas para MOTORCYCLE e CAR';

CREATE TABLE admins (
    id              UUID          PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    employee_code   VARCHAR(50)   NOT NULL UNIQUE,
    department      VARCHAR(100),
    is_super_admin  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(100),
    last_updated_by VARCHAR(100)
);

-- 3. Perfis dos usuários de demonstração (V2), pelo papel que tinham. Os
--    documentos são de demonstração, válidos pelos dígitos verificadores, e só
--    existem para a seed — dado de usuário real nunca é inventado (passo 4).
INSERT INTO owners (id, cnpj, legal_name, business_phone, created_at, last_updated_at, created_by, last_updated_by)
SELECT u.id, '04252011000110', 'Dono do Restaurante Demonstração Ltda', '1131234567', NOW(), NOW(), 'system', 'system'
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id AND r.name = 'ROLE_OWNER'
WHERE u.id = 'a0000000-0000-4000-8000-000000000001';

INSERT INTO clients (id, cpf, phone, birth_date, created_at, last_updated_at, created_by, last_updated_by)
SELECT u.id, '52998224725', '11912345678', NULL, NOW(), NOW(), 'system', 'system'
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id AND r.name = 'ROLE_CUSTOMER'
WHERE u.id = 'a0000000-0000-4000-8000-000000000002';

INSERT INTO admins (id, employee_code, department, is_super_admin, created_at, last_updated_at, created_by,
                    last_updated_by)
SELECT u.id, 'ADM-0001', 'Operações', TRUE, NOW(), NOW(), 'system', 'system'
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id AND r.name = 'ROLE_ADMIN'
WHERE u.id = 'a0000000-0000-4000-8000-000000000003';

-- 4. Todo usuário precisa de ao menos um perfil. Para os que não são da seed, o
--    banco não tem o que o perfil exige (CPF, CNPJ, telefone), e a migration não
--    inventa: falha, dizendo quantos são. Em ambiente de desenvolvimento, recriar
--    o banco (docker compose down -v) aplica tudo do zero, só com a seed.
DO $$
DECLARE
    sem_perfil INTEGER;
BEGIN
    SELECT count(*) INTO sem_perfil
    FROM users u
    WHERE NOT EXISTS (SELECT 1 FROM owners o WHERE o.id = u.id)
      AND NOT EXISTS (SELECT 1 FROM clients c WHERE c.id = u.id)
      AND NOT EXISTS (SELECT 1 FROM couriers k WHERE k.id = u.id)
      AND NOT EXISTS (SELECT 1 FROM admins a WHERE a.id = u.id);
    IF sem_perfil > 0 THEN
        RAISE EXCEPTION 'V6: % usuário(s) sem perfil. O modelo exige dados que o banco não tem (CPF, CNPJ, '
            'telefone) e a migration não os inventa. Cadastre os perfis antes de migrar ou, em '
            'desenvolvimento, recrie o banco.', sem_perfil;
    END IF;
END $$;

-- 5. O catálogo de papéis sai: o papel passa a ser derivado do perfil.
DROP TABLE user_roles;
DROP TABLE roles;
