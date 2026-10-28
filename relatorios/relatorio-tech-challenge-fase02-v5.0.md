# Relatório Técnico — Tech Challenge Fase 2

## Sistema de Gestão de Restaurantes

**Aluna:**

- Ana Melissa

**Curso:** Pós-Tech — Arquitetura e Desenvolvimento Java  
**Stack:** Java 21 · Spring Boot 3.5.x · Spring Data JPA · PostgreSQL · Flyway  
**Arquitetura:** Clean Architecture  
**Versão do relatório:** 5.0  
**Escopo desta versão:** alterações da Etapa 28 — tipos de cozinha na branch `etapa-28-cuisines`  
**Branch:** `etapa-28-cuisines` (desenvolvida em `feat/cuisines` e atualizada com a `main` em `784cada`)  
**Migrations da etapa:** `V15__cuisines.sql`, `V16__seed_cuisines.sql` e `V17__cuisine_name_collation.sql`

## Objetivo

O enunciado lista o tipo de cozinha entre os campos obrigatórios do restaurante, e o modelo de dados
v2 o prevê em duas tabelas: `cuisines`, o catálogo, e `restaurant_cuisines`, a ligação entre restaurante
e tipo (relação N:N, que ganha tabela própria). Esta etapa implementa as duas nas quatro camadas,
seguindo o padrão já aplicado ao usuário, ao restaurante e ao produto, e acrescenta à listagem pública
de restaurantes o filtro por tipo de cozinha.

## Escopo funcional

| Operação | Endpoint | Acesso | Resultado |
| --- | --- | --- | --- |
| Listar tipos | `GET /api/v1/cuisines?name=&page=&size=&sort=` | Público | `200 OK`, página HATEOAS |
| Consultar tipo | `GET /api/v1/cuisines/{id}` | Público | `200 OK`; `404` |
| Criar tipo | `POST /api/v1/cuisines` | Administrador | `201 Created`; `400`; `409` nome repetido |
| Renomear tipo | `PUT /api/v1/cuisines/{id}` | Administrador | `200 OK`; `404`; `409` nome repetido |
| Excluir tipo | `DELETE /api/v1/cuisines/{id}` | Administrador | `204 No Content`; `404`; `409` `recurso-em-uso` |
| Filtrar restaurantes | `GET /api/v1/restaurants?cuisineId=` | Público | `200 OK`, só os restaurantes que servem o tipo; combina com `name` e `ownerId` |

No restaurante, `cuisineIds` é opcional. No `POST`, ausente quer dizer nenhum tipo; no `PUT`, ausente
mantém os tipos, uma lista troca a lista inteira e a lista vazia tira todos (a mesma leitura do
`userId` na Etapa 23). Id que não está no catálogo é `404`, e nada é gravado. A resposta traz
`cuisines` (id e nome), em ordem alfabética do português.

## Alterações por camada

| Camada | Alterações |
| --- | --- |
| **Domínio** | Entidade `Cuisine` (nome obrigatório). `Restaurant` guarda a lista de tipos: `replaceCuisines` recusa nulos, repetidos e tipos sem id, e ordena pelo `Collator` `pt-BR`. |
| **Aplicação** | Casos de uso `Create`, `Update`, `FindById`, `Search` e `DeleteCuisineUseCase`; DTOs em `application/dto/cuisine`; porta `ICuisineGateway`. Políticas `UniqueCuisineNamePolicy` (criação e alteração) e `ExistingCuisinesPolicy` (cadastro e atualização do restaurante). `SearchRestaurantsUseCase` recebe o `cuisineId`. |
| **Adaptadores** | `CuisineController`, `CuisineGateway`, `CuisinePresenter`/`CuisineView`, `ICuisineDataSource`/`CuisineData`. `RestaurantGateway` e `RestaurantPresenter` reaproveitam a tradução do tipo. |
| **API REST** | `CuisineRestController`, `CreateCuisineRequest`, `UpdateCuisineRequest`, `CuisineResponse`, `CuisineModelAssembler`; `cuisineIds` nos requests do restaurante e `cuisines` na resposta; `ApiRoutes.CUISINES` e leitura pública na `SecurityConfig`. |
| **Persistência** | `CuisineJpaEntity`, `SpringDataCuisineRepository` e `CuisineDataSourceJpa` em `persistence/jpa/cuisine`; `@ManyToMany` em `RestaurantJpaEntity`; consultas paginadas de ids com o filtro por tipo. |
| **Composição** | `CompositionConfig` cria o `CuisineController` e passa a origem de tipos ao `RestaurantController`. |

## Modelo e persistência

```sql
-- V15__cuisines.sql (resumo)
CREATE TABLE cuisines (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    -- created_at, last_updated_at, created_by, last_updated_by
);
CREATE UNIQUE INDEX uk_cuisines_name_lower ON cuisines (LOWER(name));

CREATE TABLE restaurant_cuisines (
    restaurant_id UUID NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    cuisine_id    UUID NOT NULL REFERENCES cuisines (id)    ON DELETE RESTRICT,
    PRIMARY KEY (restaurant_id, cuisine_id)
);
```

A `V16__seed_cuisines.sql` traz o catálogo inicial (Brasileira, Italiana, Japonesa, Árabe, Mexicana,
Vegetariana, Hamburgueria e Pizzaria), com ids fixos `c0000000-0000-4000-8000-00000000000N`, para que o
restaurante possa escolher tipos logo depois do `docker compose up`.

A `V17__cuisine_name_collation.sql` define a collation `pt-BR-x-icu` na coluna `cuisines.name`. Sem ela, o
PostgreSQL da imagem Alpine ordena por byte, e a listagem do catálogo (`GET /api/v1/cuisines`) punha "Árabe"
depois de "Vegetariana" e as minúsculas depois de todas as maiúsculas. Com a regra do português no schema,
todo `ORDER BY name` fica na mesma ordem que o `Collator` do domínio usa na lista do restaurante.

Na JPA, `persistence/jpa/cuisine` não conhece o restaurante: o `@ManyToMany` fica só em
`RestaurantJpaEntity`, sem cascata (o tipo é do catálogo), com `@BatchSize` e fora do `@EntityGraph` —
um `Set` carregado junto da lista de horários repetiria os horários. Por isso, "algum restaurante usa este
tipo?" é uma consulta nativa na tabela de ligação. O filtro `?cuisineId=` entra na consulta paginada de
ids (`join r.cuisines`), e a carga continua pelo grafo.

### Atualização com a `main`

A etapa foi desenvolvida na branch `feat/cuisines`, com as migrations numeradas V11 e V12. Nesse meio
tempo, a `main` recebeu as Etapas 26 (endereços e catálogo geográfico, V11 e V12) e 27 (produtos, V13 e
V14). No merge:

- as migrations de cozinha passaram a **V15 e V16**, sem mudar o conteúdo. Um banco local que já tinha
  aplicado as antigas V11 e V12 de cozinha precisa ser recriado: o Flyway recusa duas migrations
  diferentes com a mesma versão;
- `RestaurantDataSourceJpa` passou a receber o `CityReferenceResolver` (endereços) e o repositório de tipos;
- os corpos de restaurante da coleção Postman passaram a usar `cityId` no endereço;
- os testes de produto passaram a montar o restaurante com a lista de tipos, e o
  `OpenApiDocumentationIT`, a conferir 32 operações (20 protegidas).

## Decisões de arquitetura

| Decisão | Conceito atendido |
| --- | --- |
| `cuisines` e `restaurant_cuisines`, com chave composta, `CASCADE` no restaurante e `RESTRICT` no tipo | Date: relação N:N vira tabela de ligação, e o par não se repete. Integridade referencial: a ligação é do restaurante; o restaurante não aponta para o que não existe. |
| Nome único **sem diferenciar maiúsculas**: índice em `LOWER(name)` e `UniqueCuisineNamePolicy` | Machado/Date: a unicidade é garantida pelo banco, contra duas requisições simultâneas, e a aplicação confere antes para devolver a mensagem certa — como CPF e CNPJ (Etapa 22). |
| `Cuisine` como agregado próprio; a regra do conjunto (`replaceCuisines`) na raiz do restaurante | Martin, *Clean Architecture*: a invariante que vale sempre mora na entidade; a regra sobre a lista mora na raiz, como os horários (Etapa 24). O restaurante se liga ao tipo, mas não é dono dele. |
| Ordem do `Collator` `pt-BR` | `String.CASE_INSENSITIVE_ORDER` compara códigos de caractere e mandava "Árabe" para o fim da lista. O teste que encontrou o problema ficou como proteção. |
| Collation `pt-BR-x-icu` em `cuisines.name` (V17) | A ordem é regra de apresentação do dado e mora no schema, que é do Flyway: uma regra só para todo `ORDER BY name`, sem código de ordenação na consulta. Consistência com a ordem que o domínio já aplica (princípio orientador do projeto). |
| Políticas em `application/policy/cuisine` e `application/policy/restaurant` | Regra de aplicação compartilhada por dois casos de uso fica num lugar só (DRY), como na Etapa 22. |
| Tipo em uso não sai do catálogo (`ResourceInUseException` → 409) | Cockburn: um objetivo do ator por caso de uso; o recurso de que outro depende não sai, como o dono com restaurante. |
| Leitura pública, escrita só do administrador | O catálogo é de todos os restaurantes, e não de um dono (Etapa 7). |
| `@ManyToMany` só do lado do restaurante; consulta nativa para "em uso" | ADP: sem ciclo entre os pacotes `restaurant` e `cuisine`. |
| Filtro por tipo na consulta paginada de ids | Paginação em duas consultas (Etapa 5); links de navegação repetem o filtro (Etapa 7). |

## Testes e validação

Foram adicionados testes unitários de todas as classes novas e alteradas (entidade, políticas, casos de
uso, gateway, presenter, controller de adaptação, origem JPA, assembler e controller REST) e os testes
de integração `CuisineLifecycleIT` e `RestaurantCuisinesIT` (7 casos, incluindo o filtro), além dos casos
de V15 a V17 no `SchemaMigrationIT`.

| Verificação executada | Resultado |
| --- | --- |
| `mvn verify` | **939 testes unitários** e **164 de integração**, sem falhas; cobertura unitária **100%** — 2766/2766 linhas e 626/626 ramos. |
| Regras de arquitetura | 48 regras ArchUnit sem violação; nenhuma regra nova foi necessária. |
| Postman (`npx newman@6`) | **169 requests e 343 asserções, sem falhas**, contra a pilha do Docker Compose; prints regenerados da mesma execução. |

**Correção feita na verificação.** A `main` traz `server.port: 8081` no `application.yml` (commit `52e3507`,
Etapa 26) para a execução fora do Docker, mas o `docker-compose.yml`, o `EXPOSE` e o `HEALTHCHECK` do
`Dockerfile` esperam 8080: o `docker compose up` deixava a aplicação *unhealthy* e inacessível. O
`docker-compose.yml` passou a fixar `SERVER_PORT: 8080` no serviço `app` (porta interna fixa, como as demais).
Com isso, a pilha do Compose sobe *healthy* e a coleção roda nela sem ajustes; fora do Docker a aplicação
continua na 8081.

## Documentação e coleção Postman

A branch atualiza o README, o changelog, as orientações do projeto (`CLAUDE.md`) e os documentos das
camadas em `docs/arquitetura/` (01, 02 e 04). Em `docs/modelo-dados/README.md`, as duas tabelas saem de "etapas
futuras" e ganham o quadro modelo × schema da V15. A documentação OpenAPI inclui as operações do catálogo,
segurança, respostas de sucesso e categorias de erro.

A coleção `postman/Restaurantes.postman_collection.json` ganhou a pasta **"19 Tipos de cozinha"**, com
19 requests (números 146 a 164): cadastro, consulta, listagem, renomeação e exclusão do tipo, com cada
erro previsto; cadastro de restaurante com tipo; o filtro `?cuisineId=` (request 158); tipo inexistente;
tipo em uso; e a limpeza do que a pasta criou. A pasta não altera a seed.

## Resultado

A branch `etapa-28-cuisines` entrega o catálogo de tipos de cozinha, a escolha dos tipos pelo
restaurante e o filtro da listagem por tipo, atualizada com as Etapas 26 e 27 da `main`.
