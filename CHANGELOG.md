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
