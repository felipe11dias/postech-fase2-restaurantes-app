-- ============================================================================
-- V17 — Ordem alfabética do português no nome do tipo de cozinha.
--
-- Sem collation própria, a coluna ordena pela do banco, que na imagem Alpine do
-- PostgreSQL compara por byte: "Árabe" ia para depois de "Vegetariana", e minúsculas
-- depois de todas as maiúsculas. Com "pt-BR-x-icu" (ICU), todo ORDER BY name segue a
-- ordem do português, a mesma do Collator pt-BR que o domínio usa na lista do restaurante.
--
-- Migration própria, e não ajuste na V15, porque migration já aplicada não é editada.
-- O índice único em LOWER(name) é reconstruído pelo ALTER; a unicidade não muda.
-- ============================================================================

ALTER TABLE cuisines
    ALTER COLUMN name TYPE VARCHAR(100) COLLATE "pt-BR-x-icu";
