# Changelog — Restaurantes (Fase 2)

## Etapa 1 — Setup do Projeto e Estrutura de Pacotes
- Projeto Maven (Spring Boot 3.5.11, Java 21) com Spring Web, Validation, HATEOAS, Actuator,
  Data JPA, PostgreSQL, Flyway, Security, jjwt, Mail, springdoc; Testcontainers, ArchUnit,
  JaCoCo, Surefire (unitários) e Failsafe (`*IT`, integração).
- Estrutura de pacotes da Clean Architecture (`domain`, `application`, `adapter`,
  `infrastructure`), cada pacote com `package-info.java` documentando sua regra.
- `ArchitectureTest` com 8 regras (regra de dependência + convenções de nome), ativas desde
  o primeiro commit com `allowEmptyShould(true)`.
- JaCoCo `check` exigindo 100% de linhas e ramos na fase `verify`.
- `application.yml` (JPA `ddl-auto: validate`, `open-in-view: false`, Flyway, JWT, mail,
  Actuator), Dockerfile multi-stage, `docker-compose.yml`, `.env.example`.

## Etapa 2 — Camada de Entidades (domínio)
- `Guard` (invariantes), VOs `Email` e `ZipCode` (records normalizados), `RoleName`, `Role`
  (igualdade por nome), `Address`, `User` (raiz do agregado, `create`/`restore`),
  `PasswordResetToken` (instante por parâmetro, uso único).
- `DomainException` e as seis exceções de domínio.
- 111 testes unitários sem mocks; cobertura 100% (170 linhas, 44 ramos) no pacote `domain`.
- Correção durante os testes: guarda de elemento nulo trocada de `contains(null)` para
  `stream().noneMatch(Objects::isNull)`, pois coleções imutáveis lançam NPE.

## Etapa 3 — Camada de Casos de Uso e Gateways
- DTOs de aplicação: `PageRequest`/`PageResult` (paginação própria, sem Spring), `AddressDTO`
  (com conversão para o domínio), `NewUserDTO`, `UpdateUserDTO`, `ChangePasswordDTO`,
  `CredentialsDTO`, `ResetPasswordDTO`, `IssuedToken`, `SortDirection`.
- Sete interfaces de gateway: `IUserGateway`, `IRoleGateway`, `IPasswordResetTokenGateway`,
  `IPasswordEncoder`, `ITokenIssuer`, `IMailGateway` e `ISecureTokenGenerator` (nova, para
  geração/hash de token de redefinição fora do núcleo e determinística em teste).
- Nove casos de uso com `create(...)`/`run(...)`: registro, atualização, troca de senha,
  exclusão, consulta por id, busca paginada, autenticação, esqueci/redefinir senha.
- 78 testes unitários com Mockito e `Clock.fixed`; cobertura acumulada 100%.
- Correções da revisão de código: `ResetPasswordUseCase` invalida o token antes de gravar a
  senha; elementos nulos em `roles`/`addresses` viram 400 em vez de NPE; `AuthenticateUseCase`
  compara contra um hash fictício quando o login não existe (mesmo tempo de resposta) e trata
  senha em branco como credencial inválida; `ChangePasswordUseCase` trata senha atual em branco
  como incorreta.
- Revisão de arquitetura: `IPasswordEncoder.simulateMatch` substitui o hash BCrypt constante que
  havia entrado no caso de uso (o núcleo volta a não conhecer o algoritmo); pacotes de
  `domain`/`application` reorganizados em subpacotes por agregado/feature (`user`, `auth`,
  `address`, `common`) — *screaming architecture*.
- Correção: `AuthenticateUseCase` volta a aparar o login antes da busca, como fazem os casos
  de uso de escrita (login com espaços nas bordas era aceito no cadastro e recusado no login).

## Etapa 4 — Adaptadores de Interface
- Porta `IUnitOfWork` em `application/gateway`: o controller de adaptação envolve cada caso de uso;
  implementação transacional fica para a infraestrutura.
- `adapter/datasource`: `IUserDataSource`, `IRoleDataSource`, `IPasswordResetTokenDataSource` e os
  records `UserData`, `RoleData`, `AddressData`, `PasswordResetTokenData`.
- `adapter/gateway`: `UserGateway`, `RoleGateway`, `PasswordResetTokenGateway` (tradução entidade ↔ record).
- `adapter/presenter`: `UserPresenter`, `AuthPresenter` e as views `UserView`, `RoleView`, `AddressView`,
  `AuthView` (sem hash de senha, por construção).
- `adapter/controller`: `UserController` e `AuthController`.
- ArchUnit: três regras novas (sufixos `Data`/`View`; gateways do adapter implementam porta do núcleo).
- 45 testes unitários; cobertura acumulada 100% (487 linhas, 126 ramos).

## Etapa 5 — Persistência com JPA (infraestrutura)
- Entidades JPA separadas do domínio: `UserJpaEntity`, `RoleJpaEntity`,
  `PasswordResetTokenJpaEntity` (`infrastructure/persistence/user`) e `AddressJpaEntity`
  (`infrastructure/persistence/address`), com `@OneToMany` cascade/orphanRemoval, `@ManyToMany`
  via `user_roles` e ids `UUID`.
- `AuditableJpaEntity`: instante (`created_at`/`last_updated_at`) vem do núcleo; autor
  (`created_by`/`last_updated_by`) é preenchido pelo `AuditingEntityListener` a partir de
  `AuthenticatedAuditorAware` (login autenticado ou `system`).
- Repositórios Spring Data e as três origens de dados `*DataSourceJpa`, implementando as
  interfaces de `adapter/datasource`.
- Busca paginada em duas consultas (ids paginados no banco + carga da página com
  `@EntityGraph`), evitando paginação em memória; ordenação traduzida por mapa
  propriedade do núcleo → atributo JPA, com `password` sempre rejeitado.
- `TransactionalUnitOfWork`: implementação de `IUnitOfWork` com `TransactionTemplate`;
  nenhum caso de uso anotado com `@Transactional`.
- `PersistenceConfig` com `@EnableJpaAuditing`.
- ArchUnit: três regras novas (`@Entity` só em `infrastructure.persistence` e com sufixo
  `JpaEntity`; sufixo exclusivo do pacote; implementações de origem de dados terminam em
  `DataSourceJpa`).
- Correção na regra de anéis concêntricos: `adapter` e `infrastructure` eram declarados como
  adaptadores irmãos, o que proibia a dependência legítima Frameworks & Drivers → Adaptadores
  de Interface. `infrastructure` passa a ser o único adaptador do DSL.
- 48 testes unitários com repositórios mockados; cobertura acumulada 100%
  (670 linhas, 140 ramos, 300 métodos, 60 classes) em 290 testes.

## Etapa 6 — Migrations e Seeds (Flyway)
- `V1__create_schema.sql`: DDL de `roles`, `users`, `user_roles`, `addresses` e
  `password_reset_tokens` — PKs `UUID` com `DEFAULT gen_random_uuid()`, colunas de auditoria,
  FKs com `ON DELETE CASCADE`, índice funcional `LOWER(name)` para a busca paginada e os dois
  índices de FK; seed do catálogo com os três papéis de `RoleName`.
- `V2__seed_demo_users.sql`: usuários de demonstração (`dono.restaurante`, `cliente.demo`,
  `admin.demo`) com senha em hash BCrypt, ids fixos, vínculo de papel resolvido pelo nome e um
  endereço para o dono.
- Primeiros testes de integração: `IntegrationTestSupport` (contexto Spring completo sobre
  PostgreSQL 16 do Testcontainers, sem nenhum bean mockado) e as classes `SchemaMigrationIT`,
  `UserPersistenceIT`, `PasswordResetTokenPersistenceIT` e `TransactionalUnitOfWorkIT`.
- Container único compartilhado pelas classes (iniciado no carregamento da classe base): com
  `@Testcontainers`/`@Container` o JUnit encerrava o container após a primeira classe e as
  seguintes reaproveitavam o contexto Spring apontando para um banco morto.
- `mvn verify`: 290 testes unitários e 27 de integração, BUILD SUCCESS; cobertura unitária
  permanece 100% (670 linhas, 140 ramos).

## Etapa 7 — API REST, Segurança e JWT (infraestrutura)
- `infrastructure/web/user` e `infrastructure/web/auth`: `UserRestController` e
  `AuthRestController` (`/api/v1`), DTOs `*Request`/`*Response` com Bean Validation e
  `UserModelAssembler` (HATEOAS, `EntityModel`/`PagedModel`).
- `infrastructure/security`: `BCryptPasswordAdapter` (`IPasswordEncoder`), `JwtTokenIssuer`
  (`ITokenIssuer`, emite e lê o token, relógio injetado), `JwtAuthenticationFilter`,
  `AuthenticatedUser`, `UserSecurity` (regra de posse) e `SecureRandomTokenGenerator`
  (`ISecureTokenGenerator`, 32 bytes + SHA-256).
- `infrastructure/mail`: `SmtpMailGateway` (`IMailGateway`) e `MailProperties`.
- `infrastructure/config`: `SecurityConfig` (stateless, sem CSRF, `@PreAuthorize`,
  `401` para não autenticado via `HttpStatusEntryPoint`) e `CompositionConfig` (raiz de
  composição: `Clock`, `UserController`, `AuthController`).
- Correção de desenho na auditoria (Etapa 5): `created_at`/`last_updated_at` passam a ser
  escritos pelo `AuditingEntityListener` (`@CreatedDate`/`@LastModifiedDate`), com o instante
  vindo do novo `ClockDateTimeProvider` ligado ao `Clock` da aplicação. O texto anterior dizia
  que o instante vinha do núcleo, mas `User.create` não recebe instante — o primeiro cadastro
  real falhava com `created_at` nulo. A origem de dados não escreve mais essas colunas, e há
  teste guardando a regra.
- Correção na configuração de segurança: o encaminhamento interno para `/error` precisa ser
  liberado, senão todo erro da aplicação vira um `403` sem corpo, escondendo a causa.
- 57 testes unitários novos (347 no total) e 18 de integração por HTTP real (46 no total),
  incluindo `401`/`403` de posse e o ciclo completo de recuperação de senha; cobertura
  unitária permanece 100% (833 linhas, 184 ramos).
- Correções da revisão de código da etapa (seis achados, dois de segurança):
  - `GET /api/v1/users` restrito a `ROLE_ADMIN`: aberto a qualquer autenticado, devolvia
    e-mail, login e endereço de todos os cadastros, anulando a regra de posse.
  - `JWT_SECRET` obrigatório: sem valor padrão no `application.yml` e no `docker-compose.yml`;
    `JwtProperties` recusa também o valor de exemplo publicado. A aplicação não sobe sem um
    segredo próprio. Testes de integração usam `IntegrationTestProperties`.
  - `@ValidPassword` (mínimo 8 caracteres, máximo 72 **bytes** UTF-8) substitui
    `@Size(max = 72)`, que contava caracteres: senha acentuada de 80 bytes passava na borda e
    fazia o BCrypt lançar exceção.
  - `UserDataSourceJpa` usa `saveAndFlush`: o `PUT` respondia com o `lastUpdatedAt` anterior
    à edição, porque o listener só carimba no flush. `ClockDateTimeProvider` passa a carimbar
    em microssegundos, a precisão do `timestamp` do PostgreSQL, para a resposta coincidir com
    o valor gravado.
  - `IMailGateway` declara que falha de transporte não se propaga, e `SmtpMailGateway`
    registra e segue: com SMTP fora do ar, o "esqueci minha senha" respondia `500` só para
    e-mail cadastrado, revelando quem tem conta.
  - `JwtTokenIssuer.read` recusa token sem `sub` ou `login` em vez de lançar
    `NullPointerException`.
  - `mvn verify`: 363 testes unitários e 50 de integração; cobertura unitária 100% (848
    linhas, 196 ramos).

## Etapa 8 — Tratamento de Erros (ProblemDetail)
- `infrastructure/web/error`: `GlobalExceptionHandler` (`@RestControllerAdvice` estendendo
  `ResponseEntityExceptionHandler`), `ProblemDetailFactory` e o catálogo `ProblemType` — toda
  resposta de erro sai em ProblemDetail (RFC 9457) com `type` próprio por categoria
  (`urn:restaurantes:problema:…`), `title`, `status`, `detail`, `instance` e `timestamp`.
- `JwtAuthenticationEntryPoint`: o 401 sem token também em ProblemDetail, com o mesmo detalhe
  para token ausente, expirado ou adulterado.
- `InvariantViolationException` no domínio (subclasse de `IllegalArgumentException`), lançada
  pelo `Guard` e por `RoleName.from`: a mensagem dela vai para a resposta; a de qualquer outra
  `IllegalArgumentException`, vinda de biblioteca, não.
- `DataIntegrityViolationException` → 409, cobrindo a corrida em que duas requisições passam
  juntas pela conferência de unicidade; o nome da restrição não sai na resposta.
- Mapa `errors` da Bean Validation como campo → lista ordenada de mensagens, e
  `spring.web.locale: pt_BR` fixo para as mensagens não dependerem do idioma do container.
- Pontos dos testes de integração deixados para esta etapa passam a verificar o status exato
  (401 para senha antiga, 400 `token-invalido` para token reutilizado, 404 para cadastro
  excluído).
- 26 testes unitários novos (389 no total) e `ErrorHandlingIT` com 14 casos (64 de integração);
  cobertura unitária 100% (930 linhas, 204 ramos).

## Etapa 9 — Documentação Swagger
- `OpenApiConfig`: título, versão, instruções de autenticação com os usuários de
  demonstração, tags e o esquema de segurança Bearer JWT (botão Authorize).
- `@ErrorResponse(type = ProblemType.X, …)` documenta cada erro pela categoria; o código HTTP
  sai do mesmo `ProblemType` que o `GlobalExceptionHandler` usa. `ErrorResponseOperationCustomizer`
  gera a resposta ProblemDetail com exemplo real da categoria e agrupa casos do mesmo código.
- `ProblemDetailOpenApiCustomizer`: esquema `ProblemDetail` montado à mão (o gerado da classe
  do Spring descreveria um mapa que a API não produz), com `type` enumerado do catálogo.
- `@SecurityRequirement` nas cinco operações protegidas; `@ResponseStatus(CREATED)` no cadastro,
  que antes era documentado como 200; exemplos nos corpos de requisição.
- `ApiDocumentation` em `web/doc` para não criar ciclo entre `config` e `web.user`.
- `springdoc.swagger-ui`: token preservado ao recarregar, ordem estável, duração das requisições.
- `OpenApiDocumentationIT` confronta o documento com a aplicação: o cadeado bate com a
  segurança real, os exemplos são aceitos pela API e cada exemplo de erro é coerente com o
  seu código.
- 22 testes unitários novos (411 no total) e 9 de integração (73 no total); cobertura unitária
  100% (998 linhas, 222 ramos).

## Etapa 10 — Execução com Docker Compose
- `docker-compose.yml`: `name: restaurantes-fase2` e sem `container_name`, para não
  compartilhar volume nem colidir nome de container com a Fase 1.
- Serviço `mailpit` (`axllent/mailpit:v1.31.2`): SMTP de testes com interface em
  `http://localhost:8025`, que torna a recuperação de senha utilizável localmente.
- `MAIL_HOST`/`MAIL_PORT` fixos no Compose: vindos do `.env`, o `localhost` do exemplo apontava
  para o próprio container da aplicação e os e-mails se perdiam em silêncio.
- `Dockerfile`: execução como usuário sem privilégio, `HEALTHCHECK` no `/actuator/health`,
  heap limitado a 75% da memória do container.
- `management.health.mail.enabled: false`: SMTP é opcional e não pode marcar a API como
  doente; o ajuste equivalente no `AuthApiIT` saiu.
- `.dockerignore` sem relatório/PDF; `.env.example` explica o que vale dentro e fora do Docker.
- Passo a passo executado literalmente: recusa sem segredo próprio, subida saudável, fluxo
  completo de recuperação de senha com o token lido no Mailpit, dados preservados no
  `down`/`up` e removidos só deste projeto no `down -v`.
- Correções da revisão de código da etapa (nove achados, um de segurança):
  - Portas publicadas só em `127.0.0.1`: o Mailpit (com tokens de redefinição de senha) e o
    PostgreSQL (senha de exemplo) ficavam alcançáveis pela rede local.
  - Porta do host configurável no `.env` (`DB_PORT`, `APP_PORT`, `MAIL_PORT`,
    `MAILPIT_UI_PORT`): as portas fixas colidiam com a Fase 1 rodando.
  - Credenciais de SMTP real movidas para a seção "só fora do Docker" do `.env.example`; o
    Compose não as repassa.
  - `HealthIT` (3 testes): com SMTP real inalcançável, a saúde é `UP`; pública sem detalhes;
    componentes `db` e `diskSpace`, sem `mail`. Religar o indicador faz os três falharem.
  - Documentação da saúde corrigida: conta banco e disco; o efeito do indicador de e-mail
    seria o container `unhealthy`, não reiniciado.
  - `Dockerfile`: cache do BuildKit para o `~/.m2` no lugar da camada de `go-offline`,
    `-Dmaven.test.skip=true` e healthcheck pelo código HTTP, sem `grep` no JSON.
  - Imagens com versão exata: `postgres:16.15-alpine3.24` (Compose e Testcontainers),
    `maven:3.9.16-eclipse-temurin-21-noble`, `eclipse-temurin:21.0.11_10-jre-alpine-3.23`.
  - `autenticar` dos ITs por HTTP centralizado na `WebIntegrationTestSupport`.
  - `mvn verify`: 411 testes unitários e 76 de integração; cobertura unitária 100% (998 linhas,
    222 ramos). Pilha verificada de novo contra os containers, em portas alternativas.
