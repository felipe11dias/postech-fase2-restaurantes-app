-- ============================================================================
-- V2 — Usuários de demonstração.
--
-- Existem para que a API possa ser exercitada logo após `docker compose up`, sem
-- cadastro manual. NÃO devem ir para um ambiente real: remova esta migration (ou
-- adicione uma que apague estas linhas) antes de publicar.
--
-- admin.demo é a razão principal desta seed: ROLE_ADMIN não pode ser obtido pelo
-- autocadastro público (regra do RegisterUserUseCase), então a migration é a via
-- legítima de existir um administrador nesta fase.
--
-- Senhas em hash BCrypt, como a aplicação grava — o valor em claro nunca é
-- persistido, nem em seed:
--   dono.restaurante / dono12345
--   cliente.demo     / cliente12345
--   admin.demo       / admin12345
--
-- Ids fixos para que a coleção Postman e os testes possam referenciá-los.
-- created_by = 'system': não há usuário autenticado por trás de uma migration.
-- ============================================================================

INSERT INTO users (id, name, email, login, password, created_at, last_updated_at, created_by, last_updated_by) VALUES
    ('a0000000-0000-4000-8000-000000000001', 'Dono do Restaurante', 'dono.restaurante@email.com', 'dono.restaurante',
     '$2a$10$RUqb7AxYwybQDdV5QtAQ..ppvi3mHdvzjmkzLTNrL77aUpvuXnfm2', NOW(), NOW(), 'system', 'system'),
    ('a0000000-0000-4000-8000-000000000002', 'Cliente Demonstração', 'cliente.demo@email.com', 'cliente.demo',
     '$2a$10$0Y5e2xUSN3xLcDv6yQLt4uwBw.d/urzKBT1YYJGJihtnUwCCdSAIG', NOW(), NOW(), 'system', 'system'),
    ('a0000000-0000-4000-8000-000000000003', 'Administrador Demonstração', 'admin.demo@email.com', 'admin.demo',
     '$2a$10$qyg4jq.BjUCf5ra86rcysevooJ/eKuIDSEoGxoD/F5.XiosHO1hvS', NOW(), NOW(), 'system', 'system');

-- O vínculo é resolvido pelo nome do papel: a migration não depende do id gerado em V1.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = CASE u.login
                             WHEN 'dono.restaurante' THEN 'ROLE_OWNER'
                             WHEN 'cliente.demo'     THEN 'ROLE_CUSTOMER'
                             WHEN 'admin.demo'       THEN 'ROLE_ADMIN'
                         END
WHERE u.login IN ('dono.restaurante', 'cliente.demo', 'admin.demo');

-- Um endereço para o dono, suficiente para exercitar a leitura do agregado completo.
INSERT INTO addresses (user_id, street, number, complement, neighborhood, city, state, zip_code) VALUES
    ('a0000000-0000-4000-8000-000000000001', 'Avenida Paulista', '1000', 'Sala 12', 'Bela Vista',
     'São Paulo', 'SP', '01310100');
