-- ============================================================================
-- V12 — Catálogo inicial de tipos de cozinha.
--
-- Para que os restaurantes possam escolher tipos logo após `docker compose up`, sem
-- cadastro manual. Ids fixos, como os usuários de demonstração da V2, para que a
-- coleção Postman e os testes possam referenciá-los.
-- created_by = 'system': não há usuário autenticado por trás de uma migration.
--
-- Vem numa migration própria, e não na V11, porque migration já aplicada não é editada.
-- ============================================================================

INSERT INTO cuisines (id, name, created_at, last_updated_at, created_by, last_updated_by) VALUES
    ('c0000000-0000-4000-8000-000000000001', 'Brasileira',   NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000002', 'Italiana',     NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000003', 'Japonesa',     NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000004', 'Árabe',        NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000005', 'Mexicana',     NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000006', 'Vegetariana',  NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000007', 'Hamburgueria', NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000008', 'Pizzaria',     NOW(), NOW(), 'system', 'system')
ON CONFLICT DO NOTHING;
