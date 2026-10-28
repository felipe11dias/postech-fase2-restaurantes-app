# Relatório Técnico — Tech Challenge Fase 2

## Sistema de Gestão de Restaurantes

**Aluno:**

- Rodrigo do Carmo Cruz

**Curso:** Pós-Tech — Arquitetura e Desenvolvimento Java  
**Stack:** Java 21 · Spring Boot 3.5.x · Spring Data JPA · PostgreSQL · Flyway  
**Arquitetura:** Clean Architecture  
**Versão do relatório:** 4.0  
**Escopo desta versão:** alterações da Etapa 27 — catálogo de produtos na branch `etapa-27-product`  
**Branch:** `etapa-27-product`  
**Base:** `main` (`24805db`)  
**Commits da etapa:** `c8c8911` — catálogo de produtos, imagens e opções; `9a1324c` — preço do produto com `BigDecimal`

## Objetivo

Disponibilizar o catálogo de produtos de cada restaurante, com descrição, preço, indicação de
consumo exclusivo no local, referências de imagens e grupos de opções configuráveis. A feature
segue a arquitetura existente e mantém produto, imagens e opções dentro do limite do agregado
`Product`.

## Escopo funcional

O catálogo oferece operações sob `/api/v1/restaurants/{restaurantId}/products`:

| Operação | Endpoint | Acesso | Resultado |
| --- | --- | --- | --- |
| Criar produto | `POST /api/v1/restaurants/{restaurantId}/products` | Administrador ou dono do restaurante | `201 Created` |
| Listar produtos | `GET /api/v1/restaurants/{restaurantId}/products?page=0&size=20` | Público | `200 OK`, página HATEOAS |
| Consultar produto | `GET /api/v1/restaurants/{restaurantId}/products/{productId}` | Público | `200 OK` |
| Substituir produto | `PUT /api/v1/restaurants/{restaurantId}/products/{productId}` | Administrador ou dono do restaurante | `200 OK` |
| Excluir produto | `DELETE /api/v1/restaurants/{restaurantId}/products/{productId}` | Administrador ou dono do restaurante | `204 No Content` |

O corpo de produto contém `name`, `description`, `price`, `dineInOnly`, `imagePaths` e
`optionGroups`. Cada grupo define obrigatoriedade, limites de seleção e valores; cada valor pode
ter um `additionalPrice`. O `PUT` substitui o estado enviado, inclusive imagens e grupos, em vez
de mesclar parcialmente coleções.

Imagens são referências textuais: a API recebe caminhos ou URLs em `imagePaths` e os persiste
em `images.content`. Não há upload, armazenamento ou entrega de arquivos de imagem nesta etapa.

## Alterações por camada

| Camada | Alterações |
| --- | --- |
| **Domínio** | Novo agregado `Product`, com `ProductOptionGroup` e `ProductOptionValue`; valida nome, descrição, preço, caminhos de imagem e limites/obrigatoriedade de seleção. |
| **Aplicação** | `ProductDTO` e DTOs de grupo/valor, tradução para o domínio e casos de uso de criação, consulta por id, busca paginada, atualização e exclusão. `IProductGateway` declara a porta consumida pelos casos de uso. |
| **Adaptadores** | `ProductController` coordena os casos de uso e a unidade de trabalho; `ProductGateway` traduz agregado e registros de persistência; `ProductPresenter` produz `ProductView`. |
| **API REST** | `ProductRestController`, DTOs de request/response e `ProductModelAssembler` fornecem validação HTTP, respostas HATEOAS, links de navegação, erros documentados e segurança por papel/posse. |
| **Persistência** | `ProductJpaEntity` é a raiz JPA; imagens, grupos e valores são partes em subpacotes próprios. `ProductDataSourceJpa` persiste o agregado e consulta primeiro os ids paginados, carregando as coleções depois. |
| **Composição** | `CompositionConfig` instancia e liga controller de adaptação, gateway e origem JPA; `ApiRoutes` centraliza a rota do catálogo. |

## Modelo, persistência e precisão monetária

A migration V13 cria `products`, `images`, `product_option_groups` e `product_option_values`,
com chaves estrangeiras e cascatas para as partes do produto. Também define limites de texto,
regras de preço e seleção e o índice usado na busca de produtos por restaurante. Grupos e valores
possuem colunas de auditoria no padrão de persistência do projeto.

O modelo lógico declara o preço do produto como `double`. Para evitar aproximação binária em um
valor monetário, esta branch adotou `BigDecimal` nas fronteiras da aplicação, domínio, views,
resposta HTTP e entidade JPA. A migration V14 altera a coluna `products.price` para
`numeric(10,2)`, convertendo os valores já armazenados. Domínio e API restringem o preço a valor
não negativo, até oito dígitos inteiros e duas casas decimais. O preço adicional das opções
continua sendo `BigDecimal`/`numeric(10,2)`.

O esquema é verificado pelo Flyway; a entidade JPA declara precisão e escala compatíveis com
`numeric(10,2)`. A consulta paginada usa duas operações para evitar paginação em memória de
coleções carregadas em conjunto.

## Decisões de arquitetura

| Decisão | Conceito atendido |
| --- | --- |
| Produto é a raiz; imagens, grupos e valores são partes do agregado | As invariantes e a consistência ficam sob responsabilidade da raiz, sem ciclo de vida independente para as partes. |
| Gateways e registros traduzem dados entre camadas | Dependências apontam para as portas declaradas pelos consumidores; entidades JPA e DTOs HTTP não atravessam para o domínio. |
| Rotas aninhadas ao restaurante; escrita exige administrador ou dono | A posse é verificada no recurso pai e reutiliza a regra de segurança existente contra acesso indevido a recursos alheios. |
| `PUT` substitui as coleções completas | O contrato explícito mantém determinística a reconciliação de partes sem identificadores expostos na API. |
| Busca primeiro ids e depois carrega o grafo | Evita o comportamento do Hibernate que pode paginar coleções em memória após um `join fetch`. |
| Preço usa `BigDecimal` e `numeric(10,2)` | A representação decimal preserva valores monetários sem erro de ponto flutuante binário; a migration converte o schema já existente. |

## Testes e validação

Foram adicionados testes unitários de domínio, DTO/mapeamento, casos de uso, gateway, presenter,
controller de adaptação, origem JPA, assembler HATEOAS e controller REST. Os testes de integração
cobrem o ciclo HTTP do produto e as migrations, incluindo tipo, precisão e escala de
`products.price`.

| Verificação executada | Resultado |
| --- | --- |
| `mvn test` | **851 testes unitários aprovados**; JaCoCo com **100% de linhas e ramos** — 2519/2519 linhas e 592/592 ramos. |
| `mvn verify -Dit.test=ProductCatalogIT,SchemaMigrationIT -Dtest=NoSuch -Dsurefire.failIfNoSpecifiedTests=false` | **22 testes de integração aprovados** (ciclo HTTP do produto e 21 verificações de schema/migrations); BUILD SUCCESS e gate de cobertura atendido. |

A execução direcionada acima não equivale à execução completa de todos os testes de integração da
branch.

## Documentação e coleção Postman

A branch atualiza o README, o changelog, o relatório técnico geral, as orientações do projeto,
os documentos das camadas de arquitetura e a comparação do modelo de dados com o schema. A
documentação OpenAPI inclui as operações, segurança, respostas de sucesso e categorias de erro.

A coleção `postman/Restaurantes.postman_collection.json` ganhou a pasta **“18 Produtos —
Catálogo”**, com 20 requests (números 120 a 139) para sucesso, paginação, validação, autenticação,
posse e recursos inexistentes. Os requests exercitam criação, leitura, atualização e exclusão.
Nesta validação, não foi executada a coleção com Newman nem foram gerados os prints dos requests
novos.

## Resultado

A branch `etapa-27-product` entrega o catálogo CRUD de produtos com opções configuráveis,
referências de imagens, autorização de escrita por posse e integração com o PostgreSQL. O preço
foi padronizado em `BigDecimal` ao longo das camadas, e a V14 converte a coluna para
`numeric(10,2)` sem alterar as migrations anteriores.
