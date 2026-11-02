-- ============================================================================
-- V18 — Catálogo de tipos de cozinha ampliado.
--
-- A V16 trouxe oito tipos; aqui entram outros trinta (cozinhas de outros países,
-- regionais do Brasil e categorias de estabelecimento), para que o restaurante escolha
-- os tipos logo após `docker compose up`, sem cadastro manual pelo administrador.
--
-- Ids fixos, como os da V16, para que a coleção Postman possa referenciá-los. O sufixo tem
-- dois dígitos (…010 a …039) para não se confundir com os da V16 (…001 a …008).
-- created_by = 'system': não há usuário autenticado por trás de uma migration.
--
-- ON CONFLICT DO NOTHING sem alvo cobre também o índice único em LOWER(name): se o
-- administrador já cadastrou um destes nomes pela API, a migration mantém o dele e segue.
-- Migration própria, e não ajuste na V16, porque migration já aplicada não é editada.
-- ============================================================================

INSERT INTO cuisines (id, name, created_at, last_updated_at, created_by, last_updated_by) VALUES
    ('c0000000-0000-4000-8000-000000000010', 'Açaí',           NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000011', 'Alemã',          NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000012', 'Amazônica',      NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000013', 'Argentina',      NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000014', 'Baiana',         NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000015', 'Cafeteria',      NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000016', 'Chinesa',        NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000017', 'Churrascaria',   NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000018', 'Comida caseira', NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000019', 'Confeitaria',    NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000020', 'Contemporânea',  NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000021', 'Coreana',        NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000022', 'Espanhola',      NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000023', 'Francesa',       NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000024', 'Frutos do mar',  NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000025', 'Gaúcha',         NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000026', 'Grega',          NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000027', 'Indiana',        NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000028', 'Lanches',        NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000029', 'Mineira',        NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000030', 'Nordestina',     NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000031', 'Padaria',        NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000032', 'Pastelaria',     NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000033', 'Peruana',        NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000034', 'Poke',           NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000035', 'Portuguesa',     NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000036', 'Saudável',       NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000037', 'Sorveteria',     NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000038', 'Tailandesa',     NOW(), NOW(), 'system', 'system'),
    ('c0000000-0000-4000-8000-000000000039', 'Vegana',         NOW(), NOW(), 'system', 'system')
ON CONFLICT DO NOTHING;
