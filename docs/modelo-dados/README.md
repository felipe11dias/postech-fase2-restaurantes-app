# Modelo de Dados v2

Modelo lógico do banco definido pelo autor e exportado do dbdiagram:

- [`postech-2-restaurantes.sql`](postech-2-restaurantes.sql) — DDL do modelo;
- [`postech-2-restaurantes.pdf`](postech-2-restaurantes.pdf) — diagrama ER (mesmo conteúdo).

São **documentos de referência**, não migrations: o Flyway só executa o que está em
`src/main/resources/db/migration`, e o schema físico continua sendo escrito lá, etapa por etapa.
A adequação, com o quadro antes × v2, os enums e cada etapa, está no relatório técnico — seção
"Modelo de Dados v2 — adequação" de
[`relatorio-tech-challenge-fase02-v2.0.md`](../../relatorios/relatorio-tech-challenge-fase02-v2.0.md); o
quadro final, tabela por tabela, com o schema real (V1 a V10), está na Etapa 25 do mesmo relatório.

## Escopo da adequação (Etapas 17 a 25)

`users`, `user_addresses`, `password_reset_tokens`, `owners`, `clients`, `couriers`, `admins`,
`restaurants` e `restaurant_office_hours`. As demais tabelas (`addresses` com `city_id`, `cities`,
`states`, `cuisines`, `restaurant_cuisines`, `products`, `product_option_groups`,
`product_option_values`, `images`) ficam para etapas futuras.

## Enums

| Tipo | Valores | No domínio |
| --- | --- | --- |
| `courier_vehicle_type` | `ON_FOOT`, `BICYCLE`, `MOTORCYCLE`, `CAR` | `CourierVehicleType` — `MOTORCYCLE` e `CAR` exigem CNH e placa |
| `courier_status` | `OFFLINE`, `AVAILABLE`, `BUSY` | `CourierStatus` — padrão `OFFLINE` |
| `day_of_week` | `MONDAY` … `SUNDAY` | `java.time.DayOfWeek` |

## Divergências modelo → schema físico

| Ponto do modelo | No schema físico |
| --- | --- |
| FKs `addresses (id) REFERENCES user_addresses (address_id)` e `restaurants (address_id)` | Direção invertida no export: `user_addresses.address_id` e `restaurants.address_id` referenciam `addresses (id)` |
| Auditoria, `expires_at` e `used` sem `NOT NULL` | `NOT NULL` mantido onde já existe hoje |
| `varchar` sem tamanho | Tamanho definido por coluna |
| `is_default` sem unicidade | Restrição de exclusão adiada para o commit: no máximo um padrão por usuário (`ex_user_addresses_one_default`, V4) |
| `CASCADE` de `users`/`restaurants` | Não alcança `addresses` (referenciada); a aplicação remove o endereço |
| Chaves estrangeiras `DEFERRABLE INITIALLY IMMEDIATE` | Não adiáveis: o efeito é o mesmo (conferidas a cada comando), e nenhuma transação precisa adiá-las |
| — (não está no modelo) | Garantias a mais no schema: `CHECK` de CNH e placa por tipo de veículo, `CHECK` de abertura ≠ fechamento, gatilho "um CPF, uma pessoa" entre `clients` e `couriers`, gatilho "ao menos um administrador" em `admins`, `restaurants.user_id → owners` |

## Observações para as tabelas fora do escopo

- `products.price double`: no PostgreSQL é `double precision`; valor monetário pede `numeric(10,2)`,
  como `product_option_values.additional_price`.
- `images.content blob`: no PostgreSQL, `bytea`.
- `products` e `images` não têm colunas de auditoria, ao contrário das demais tabelas.
