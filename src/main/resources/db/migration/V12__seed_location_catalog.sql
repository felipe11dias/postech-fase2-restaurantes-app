-- V12 — Catálogo inicial de estados e cidades, mantido exclusivamente por migrations.
-- Inclui as 27 unidades federativas, as capitais e Campinas. Não é a lista completa
-- de municípios brasileiros. Novas cidades entram em novas migrations.
-- Os dados legados normalizados pela V11 e seus vínculos são preservados.

INSERT INTO states (id, name, acronym) VALUES
    ('d0000000-0000-4000-8000-000000000001', 'Acre', 'AC'),
    ('d0000000-0000-4000-8000-000000000002', 'Alagoas', 'AL'),
    ('d0000000-0000-4000-8000-000000000003', 'Amapá', 'AP'),
    ('d0000000-0000-4000-8000-000000000004', 'Amazonas', 'AM'),
    ('d0000000-0000-4000-8000-000000000005', 'Bahia', 'BA'),
    ('d0000000-0000-4000-8000-000000000006', 'Ceará', 'CE'),
    ('d0000000-0000-4000-8000-000000000007', 'Distrito Federal', 'DF'),
    ('d0000000-0000-4000-8000-000000000008', 'Espírito Santo', 'ES'),
    ('d0000000-0000-4000-8000-000000000009', 'Goiás', 'GO'),
    ('d0000000-0000-4000-8000-000000000010', 'Maranhão', 'MA'),
    ('d0000000-0000-4000-8000-000000000011', 'Mato Grosso', 'MT'),
    ('d0000000-0000-4000-8000-000000000012', 'Mato Grosso do Sul', 'MS'),
    ('d0000000-0000-4000-8000-000000000013', 'Minas Gerais', 'MG'),
    ('d0000000-0000-4000-8000-000000000014', 'Pará', 'PA'),
    ('d0000000-0000-4000-8000-000000000015', 'Paraíba', 'PB'),
    ('d0000000-0000-4000-8000-000000000016', 'Paraná', 'PR'),
    ('d0000000-0000-4000-8000-000000000017', 'Pernambuco', 'PE'),
    ('d0000000-0000-4000-8000-000000000018', 'Piauí', 'PI'),
    ('d0000000-0000-4000-8000-000000000019', 'Rio de Janeiro', 'RJ'),
    ('d0000000-0000-4000-8000-000000000020', 'Rio Grande do Norte', 'RN'),
    ('d0000000-0000-4000-8000-000000000021', 'Rio Grande do Sul', 'RS'),
    ('d0000000-0000-4000-8000-000000000022', 'Rondônia', 'RO'),
    ('d0000000-0000-4000-8000-000000000023', 'Roraima', 'RR'),
    ('d0000000-0000-4000-8000-000000000024', 'Santa Catarina', 'SC'),
    ('d0000000-0000-4000-8000-000000000025', 'Sergipe', 'SE'),
    ('d0000000-0000-4000-8000-000000000026', 'São Paulo', 'SP'),
    ('d0000000-0000-4000-8000-000000000027', 'Tocantins', 'TO')
ON CONFLICT (acronym) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO cities (id, state_id, name)
SELECT seed.id::UUID, states.id, seed.name
FROM (VALUES
    ('c0000000-0000-4000-8000-000000000003', 'AC', 'Rio Branco'),
    ('c0000000-0000-4000-8000-000000000004', 'AL', 'Maceió'),
    ('c0000000-0000-4000-8000-000000000005', 'AP', 'Macapá'),
    ('c0000000-0000-4000-8000-000000000006', 'AM', 'Manaus'),
    ('c0000000-0000-4000-8000-000000000007', 'BA', 'Salvador'),
    ('c0000000-0000-4000-8000-000000000008', 'CE', 'Fortaleza'),
    ('c0000000-0000-4000-8000-000000000009', 'DF', 'Brasília'),
    ('c0000000-0000-4000-8000-000000000010', 'ES', 'Vitória'),
    ('c0000000-0000-4000-8000-000000000011', 'GO', 'Goiânia'),
    ('c0000000-0000-4000-8000-000000000012', 'MA', 'São Luís'),
    ('c0000000-0000-4000-8000-000000000013', 'MT', 'Cuiabá'),
    ('c0000000-0000-4000-8000-000000000014', 'MS', 'Campo Grande'),
    ('c0000000-0000-4000-8000-000000000015', 'MG', 'Belo Horizonte'),
    ('c0000000-0000-4000-8000-000000000016', 'PA', 'Belém'),
    ('c0000000-0000-4000-8000-000000000017', 'PB', 'João Pessoa'),
    ('c0000000-0000-4000-8000-000000000018', 'PR', 'Curitiba'),
    ('c0000000-0000-4000-8000-000000000019', 'PE', 'Recife'),
    ('c0000000-0000-4000-8000-000000000020', 'PI', 'Teresina'),
    ('c0000000-0000-4000-8000-000000000002', 'RJ', 'Rio de Janeiro'),
    ('c0000000-0000-4000-8000-000000000022', 'RN', 'Natal'),
    ('c0000000-0000-4000-8000-000000000023', 'RS', 'Porto Alegre'),
    ('c0000000-0000-4000-8000-000000000024', 'RO', 'Porto Velho'),
    ('c0000000-0000-4000-8000-000000000025', 'RR', 'Boa Vista'),
    ('c0000000-0000-4000-8000-000000000026', 'SC', 'Florianópolis'),
    ('c0000000-0000-4000-8000-000000000027', 'SE', 'Aracaju'),
    ('c0000000-0000-4000-8000-000000000001', 'SP', 'São Paulo'),
    ('c0000000-0000-4000-8000-000000000029', 'TO', 'Palmas'),
    ('c0000000-0000-4000-8000-000000000030', 'SP', 'Campinas')
) AS seed(id, acronym, name)
JOIN states ON states.acronym = seed.acronym
WHERE NOT EXISTS (
    SELECT 1 FROM cities existing
    WHERE existing.state_id = states.id AND LOWER(existing.name) = LOWER(seed.name)
)
ON CONFLICT (state_id, name) DO NOTHING;
