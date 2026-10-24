# Modelo de Dados v2

Modelo lógico do banco definido pelo autor e exportado do dbdiagram:

- [`postech-2-restaurantes.sql`](postech-2-restaurantes.sql) — DDL do modelo;
- [`postech-2-restaurantes.pdf`](postech-2-restaurantes.pdf) — diagrama ER (mesmo conteúdo).

São **documentos de referência**, não migrations: o Flyway só executa o que está em
`src/main/resources/db/migration`, e o schema físico continua sendo escrito lá, etapa por etapa.
A adequação, com o quadro antes × v2, os enums e cada etapa, está no relatório técnico — seção
"Modelo de Dados v2 — adequação" de
[`relatorio-tech-challenge-fase02-v2.0.md`](../../relatorios/relatorio-tech-challenge-fase02-v2.0.md); o
quadro final, tabela por tabela, com o schema real até V10, está na Etapa 25; a V11 e as tabelas
de produtos estão documentadas na Etapa 26 do mesmo relatório.

## Escopo implementado (Etapas 17 a 26)

`users`, `user_addresses`, `password_reset_tokens`, `owners`, `clients`, `couriers`, `admins`,
`restaurants`, `restaurant_office_hours`, `products`, `product_option_groups`,
`product_option_values` e `images`. As tabelas `addresses` com `city_id`, `cities`, `states`,
`cuisines` e `restaurant_cuisines` permanecem para etapas futuras.

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

## Produtos: diagrama do modelo × schema da V13

As quatro tabelas abaixo foram conferidas no diagrama PDF fornecido e no DDL exportado do modelo.
Os nomes das colunas e as relações foram mantidos, com uma decisão alinhada à descrição da tarefa:
como o arquivo da foto não será enviado nem utilizado nesta etapa, `images.content` guarda somente
o caminho/URL como texto, não o blob do modelo lógico. O schema acrescenta limites e restrições
físicas para reforçar as invariantes.

| Tabela | Modelo lógico | Schema físico V13 | Conformidade |
| --- | --- | --- | --- |
| `products` | `id`, `restaurant_id`, `name`, `description`, `price double`, `is_dine_in_only` | Mesmas colunas; `price double precision`; FK `restaurant_id → restaurants` com `ON DELETE CASCADE` | Conforme o modelo |
| `images` | `id`, `product_id`, `content blob` | Mesmas colunas; `content varchar(2048)`; FK para `products` com cascata | Desvio intencional para atender à tarefa: persiste referência textual; API recebe/devolve `imagePaths` |
| `product_option_groups` | `id`, `product_id`, `name`, `is_required`, `min_selections`, `max_selections` e auditoria | Mesmas colunas e FK para `products` com cascata; timestamps obrigatórios | Estrutura mantida; timestamps `NOT NULL` seguem o padrão de auditoria do projeto |
| `product_option_values` | `id`, `option_group_id`, `name`, `additional_price numeric(10,2)` e auditoria | Mesmas colunas e FK para `product_option_groups` com cascata | Estrutura, relação e precisão monetária mantidas |

**Tipos monetários conforme o modelo.** `products.price` usa `double` no domínio/JPA e
`double precision` no PostgreSQL, conforme a definição explícita do modelo. `additional_price`
permanece `BigDecimal`/`numeric(10,2)`, também como definido pelo modelo.

**Complementos físicos.** O DDL original deixa `varchar` sem limite e os campos de auditoria
anuláveis. A V11 limita nomes/descrições, acrescenta `CHECK`s para preços e limites de seleção,
e torna `created_at`/`last_updated_at` obrigatórios nas tabelas auditáveis. O índice em
`products.restaurant_id` atende à listagem por restaurante. As FKs mantêm as cascatas declaradas
pelo modelo.

**Auditoria:** o modelo não declara colunas de auditoria em `products` nem `images`; elas não foram
adicionadas. `product_option_groups` e `product_option_values` incluem as quatro colunas de
auditoria do modelo e usam `AuditableJpaEntity`.
