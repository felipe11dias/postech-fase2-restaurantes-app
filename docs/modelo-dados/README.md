# Modelo de Dados v2

Modelo lógico do banco definido pelo autor e exportado do dbdiagram:

- [`postech-2-restaurantes.sql`](postech-2-restaurantes.sql) — DDL do modelo;
- [`postech-2-restaurantes.pdf`](postech-2-restaurantes.pdf) — diagrama ER (mesmo conteúdo).

São **documentos de referência**, não migrations: o Flyway só executa o que está em
`src/main/resources/db/migration`, e o schema físico continua sendo escrito lá, etapa por etapa.
A adequação, com o quadro antes × v2, os enums e cada etapa, está no relatório técnico — seção
"Modelo de Dados v2 — adequação" de
[`relatorio-tech-challenge-fase02-v2.0.md`](../../relatorios/relatorio-tech-challenge-fase02-v2.0.md); o
quadro final, tabela por tabela, com o schema real até V10, está na Etapa 25 do mesmo relatório. A
normalização do endereço por cidade e estado (V11/V12) está na Etapa 26
([v3.0](../../relatorios/relatorio-tech-challenge-fase02-v3.0.md)), as tabelas de produtos (V13/V14) na
Etapa 27 ([v4.0](../../relatorios/relatorio-tech-challenge-fase02-v4.0.md)) e os tipos de cozinha
(V15 a V17) na Etapa 28 ([v5.0](../../relatorios/relatorio-tech-challenge-fase02-v5.0.md)). O
[relatório final](../../relatorios/relatorio-tech-challenge-fase02-final.md) resume tudo e traz as
divergências numa tabela só.

## Escopo implementado (Etapas 17 a 28)

`users`, `user_addresses`, `password_reset_tokens`, `owners`, `clients`, `couriers`, `admins`,
`restaurants`, `restaurant_office_hours`, `addresses` com `city_id`, `cities` e `states` (Etapa 26),
`products`, `product_option_groups`, `product_option_values` e `images` (Etapa 27), e `cuisines` e
`restaurant_cuisines` (Etapa 28).

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

## Localidades: modelo × schema da V11 e da V12

| Tabela / ponto | No modelo | No schema físico (V11) | Por quê |
| --- | --- | --- | --- |
| `states` | `name` e `acronym` `varchar` anuláveis | `name VARCHAR(100) NOT NULL`, `acronym VARCHAR(2) NOT NULL UNIQUE` | estado sem nome ou sigla não identifica nada; a sigla é a chave natural |
| `cities` | `name` `varchar` anulável | `name VARCHAR(100) NOT NULL`, `UNIQUE (state_id, name)` | a mesma cidade não pode aparecer duas vezes no mesmo estado |
| `cities.state_id → states` e `addresses.city_id → cities` | FK sem ação, `DEFERRABLE INITIALLY IMMEDIATE` | `ON DELETE RESTRICT`, não adiáveis | catálogo referenciado não sai enquanto houver endereço apontando para ele; o efeito de "imediata" é o mesmo |
| `addresses.city_id` | `NOT NULL` | `NOT NULL`, preenchida pela conversão dos textos de cidade/UF dos endereços existentes | a V11 preserva ids e vínculos de um banco na V10 |
| Carga inicial | — | V12: as 27 unidades federativas, as capitais e Campinas, com ids fixos | catálogo sem API de escrita: municípios novos entram por migration |

## Produtos: diagrama do modelo × schema da V13

As quatro tabelas abaixo foram conferidas no diagrama PDF fornecido e no DDL exportado do modelo.
Os nomes das colunas e as relações foram mantidos, com uma decisão alinhada à descrição da tarefa:
como o arquivo da foto não será enviado nem utilizado nesta etapa, `images.content` guarda somente
o caminho/URL como texto, não o blob do modelo lógico. O schema acrescenta limites e restrições
físicas para reforçar as invariantes.

| Tabela | Modelo lógico | Schema físico V13 | Conformidade |
| --- | --- | --- | --- |
| `products` | `id`, `restaurant_id`, `name`, `description`, `price double`, `is_dine_in_only` | Mesmas colunas; `price numeric(10,2)`; FK `restaurant_id → restaurants` com `ON DELETE CASCADE` | Desvio físico intencional: precisão decimal para valor monetário |
| `images` | `id`, `product_id`, `content blob` | Mesmas colunas; `content varchar(2048)`; FK para `products` com cascata | Desvio intencional para atender à tarefa: persiste referência textual; API recebe/devolve `imagePaths` |
| `product_option_groups` | `id`, `product_id`, `name`, `is_required`, `min_selections`, `max_selections` e auditoria | Mesmas colunas e FK para `products` com cascata; timestamps obrigatórios | Estrutura mantida; timestamps `NOT NULL` seguem o padrão de auditoria do projeto |
| `product_option_values` | `id`, `option_group_id`, `name`, `additional_price numeric(10,2)` e auditoria | Mesmas colunas e FK para `product_option_groups` com cascata | Estrutura, relação e precisão monetária mantidas |

**Tipos monetários.** O modelo lógico declara `products.price` como `double`, mas a implementação
usa `BigDecimal` no domínio e nas fronteiras, com `numeric(10,2)` no PostgreSQL após a V14. A
migration converte os valores existentes; o domínio aceita valores não negativos com até oito
dígitos inteiros e duas casas decimais. `additional_price` permanece `BigDecimal`/`numeric(10,2)`,
como definido pelo modelo.

**Complementos físicos.** O DDL original deixa `varchar` sem limite e os campos de auditoria
anuláveis. A V13 limita nomes/descrições, acrescenta `CHECK`s para preços e limites de seleção,
e torna `created_at`/`last_updated_at` obrigatórios nas tabelas auditáveis. O índice em
`products.restaurant_id` atende à listagem por restaurante. As FKs mantêm as cascatas declaradas
pelo modelo.

**Auditoria:** o modelo não declara colunas de auditoria em `products` nem `images`; elas não foram
adicionadas. `product_option_groups` e `product_option_values` incluem as quatro colunas de
auditoria do modelo e usam `AuditableJpaEntity`.

## Tipos de cozinha: modelo × schema da V15 e da V17

`cuisines` é o catálogo; `restaurant_cuisines`, a ligação N:N com o restaurante (Etapa 28). Nomes de colunas,
chave composta e direção das cascatas seguem o modelo.

| Tabela | Modelo lógico | Schema físico V15 | Conformidade |
| --- | --- | --- | --- |
| `cuisines` | `id`, `name varchar UNIQUE NOT NULL` e auditoria | Mesmas colunas; `name varchar(100) COLLATE "pt-BR-x-icu"` (V17); unicidade por índice em `LOWER(name)`; `created_at`/`last_updated_at` `NOT NULL` | Unicidade mais forte que a do modelo: "Italiana" e "ITALIANA" são o mesmo tipo. Tamanho e auditoria obrigatória seguem o padrão do projeto; a collation faz o catálogo ordenar pela regra do português |
| `restaurant_cuisines` | `restaurant_id`, `cuisine_id`, `PRIMARY KEY` composta, índice em `cuisine_id`; FK para `restaurants` com `CASCADE` e para `cuisines` com `RESTRICT` | Mesmas colunas, chave, índice e cascatas | Estrutura mantida; FKs não adiáveis, como nas demais tabelas (ver divergências acima) |

**Catálogo inicial.** A V16 insere oito tipos com ids fixos (`c0000000-0000-4000-8000-00000000000N`); o modelo
não traz dados.
