# Changelog — Restaurantes (Fase 2)

## Etapa 16 — Módulo de Gestão de Restaurantes (Imagem 2)
- Criação da tabela `restaurants` via Flyway migration `V3__create_restaurant_schema.sql` com chaves estrangeiras vinculadas a `users` e `addresses`.
- Criação da entidade de domínio `Restaurant` com verificação estrita de invariantes (nome, dono com perfil `ROLE_OWNER` ou `ROLE_ADMIN`, endereço do dono e horários de funcionamento).
- Casos de uso: `CreateRestaurantUseCase`, `FindRestaurantByIdUseCase`, `SearchRestaurantsUseCase`, `UpdateRestaurantUseCase` e `DeleteRestaurantUseCase`.
- Adaptadores de interface: `RestaurantController`, `RestaurantGateway`, `RestaurantPresenter` e `RestaurantView`.
- Infraestrutura: `RestaurantJpaEntity`, `SpringDataRestaurantRepository`, `RestaurantDataSourceJpa`, `RestaurantRestController` (`/api/v1/restaurants`), DTOs REST, HATEOAS `RestaurantModelAssembler`, documentação OpenAPI e registro em `CompositionConfig`.
- Teste de integração de ciclo de vida completo `RestaurantLifecycleIT` com PostgreSQL via Testcontainers.
- Testes unitários cobrindo 100% de linhas e ramos no JaCoCo.
- Documentação acadêmica detalhada em `docs/relatorio-academico-modulo-restaurantes.md`.

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

## Etapa 11 — Testes: unitários (100% de cobertura) e de integração
- Auditoria da suíte contra a especificação da etapa; tabelas do relatório reescritas com as
  classes e suítes reais.
- Gate de cobertura passa a medir **só os testes unitários**: agentes do JaCoCo separados para
  Surefire (`jacoco.exec`, gate) e Failsafe (`jacoco-it.exec`, relatório informativo em
  `target/site/jacoco-it`), ambos com `append=false`. Antes, a integração somava ao mesmo
  arquivo e o arquivo acumulava execuções anteriores.
- `UserModelAssembler`: links de navegação `self`/`first`/`prev`/`next`/`last` na listagem,
  repetindo busca e ordenação, com codificação única dos parâmetros.
- `JwtAuthenticationIT` (4): token expirado, assinado com outra chave e sem assinatura recusados;
  controle com a chave certa aceito.
- `UserSearchIT` (5): busca parcial sem diferenciar maiúsculas, navegação seguindo os links,
  ordenação decrescente, `sort=password` ignorado, tamanho de página fora do limite.
- `UserLifecycleIT` (1): cenário principal encadeado, do cadastro à exclusão, sem órfãos no banco.
- `AuthApiIT`: token de redefinição vencido. `UserApiIT`: senha e papéis preservados no `PUT` e
  autor da auditoria em cada alteração; dois testes renomeados para o que de fato verificam.
- `TestConventionsTest` (7 regras ArchUnit sobre as classes de teste) e regra de caso de uso
  reforçada: `run` como único método público de instância.
- `autenticar` da base de ITs por HTTP reaproveitado; suíte verificada em ordem aleatória com
  duas sementes.
- `mvn clean verify`: 422 testes unitários e 89 de integração; cobertura unitária 100%
  (1016 linhas, 226 ramos, 427 métodos).

## Etapa 12 — Entregáveis (Postman, README)
- `postman/Restaurantes.postman_collection.json` (v2.1): 52 requests em 9 pastas, um por caso de
  cada endpoint (sucesso e cada erro previsto), com testes de status e `type` do ProblemDetail;
  guarda `adminToken`, `token` e `userId` e lê o token de redefinição no Mailpit. Cria os
  próprios usuários e os exclui ao fim; os casos de 403 miram um cadastro descartável, nunca a
  seed; login ou cadastro essencial que falhe interrompe a execução.
- `postman/prints/`: 52 prints de uma execução do Newman, gerados por `postman/gerar-prints.js`
  (altura medida pelo Chrome headless; substitui os antigos só depois de gerar todos; request sem
  resposta vira print da falha).
- `README.md` reescrito: estado da entrega (restaurante, cardápio e CRUD de tipos pendentes),
  geração do segredo JWT, variáveis, carga do `.env` para rodar fora do Docker, usuários da seed,
  autenticação, endpoints, catálogo de erros, coleção (`newman@6`), prints e testes.
- Correções da revisão de código da etapa (dez achados): seed fora dos casos de 403, gerador de
  prints sem apagar nada antes de gerar tudo e tolerante a request sem resposta, execução
  interrompida em falha essencial, `.env` exportado explicitamente, Newman com versão fixada,
  pasta temporária removida, relatório atualizado.
- Newman: 52 requests, 108 asserções, nenhuma falha, duas execuções seguidas contra o Compose.

## Etapa 13 — Infraestrutura em módulos substituíveis
- `infrastructure/` reorganizada por papel/tecnologia: `main` (composição), `web` (`api`, `error`,
  `doc`, `validation`, `security`), `persistence/jpa` (`audit`, `user`), `token/jwt`, `crypto`,
  `mail/smtp`. 62 classes movidas com `git mv`; `config/` deixou de existir; cada módulo habilita a
  própria configuração (`JwtConfig`, `MailConfig`, ...).
- Web não conhece JWT: `BearerTokenAuthenticationFilter` (antes `JwtAuthenticationFilter`) lê pela
  porta `web/security/IAccessTokenReader`, implementada por `JwtTokenIssuer`;
  `JwtAuthenticationEntryPoint` → `ProblemDetailAuthenticationEntryPoint`.
- Persistência não conhece segurança: `AuthenticatedAuditorAware` recebe um `Supplier`;
  `web/security/AuthenticatedActor` diz quem está autenticado; `main` liga os dois.
- Ciclo `persistence.address ↔ persistence.user` eliminado: `AddressJpaEntity` em `persistence/jpa/user`.
- Validade do token de redefinição: `main/PasswordResetProperties`
  (`PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES`, antes `MAIL_RESET_TOKEN_EXPIRATION_MINUTES`) e
  `IMailGateway.sendPasswordReset(to, token, validity)` — o texto do e-mail usa a mesma validade do token.
- `InfrastructureModulesTest`: 14 regras ArchUnit (sem ciclos no projeto; módulos isolados; cada
  biblioteca no seu módulo; `*Config` ⇔ `@Configuration`), conferidas com violações propositais.
  Exclusão do JaCoCo generalizada para `**/infrastructure/**/*Config.class`.
- `mvn clean verify`: 441 testes unitários e 89 de integração; cobertura unitária 100% (1022
  linhas, 226 ramos). Newman: 52 requests, 108 asserções, nenhuma falha.

## Etapa 14 — Revisão de conformidade e documentação da arquitetura
- Revisão do relatório e da arquitetura contra as referências (Martin, Cockburn, Freeman, Date,
  Machado) e as Aulas 01 a 07 da Fase 2.
- Gateways de serviço no adaptador (Aulas 02, 05 e 06: o gateway traduz, a infraestrutura
  transporta): `adapter/service` com `IMailSender`, `ITokenEncoder` e `TokenClaimsData`;
  `PasswordResetMailGateway` (monta assunto e corpo do e-mail) e `TokenGateway` (`User` → claims)
  em `adapter/gateway`; `SmtpMailGateway` → `SmtpMailSender` (só transporte) e `JwtTokenIssuer` →
  `JwtTokenEncoder` (só codificação, sem import do domínio). `AuthController` recebe os serviços e
  cria os gateways a cada operação.
- ArchUnit: regras de prefixo `I` e de records `*Data` estendidas a `adapter.service`; regras novas
  `infraestrutura_so_conhece_portas_tecnicas` (a infraestrutura só depende de `IPasswordEncoder`,
  `ISecureTokenGenerator` e `IUnitOfWork` — pega também lambda num `@Bean`) e
  `transporte_nao_conhece_o_dominio` (`mail` e `token` sem `domain`), conferidas com violações
  propositais.
- `docs/arquitetura/`: índice e um documento por parte da Clean Architecture (visão geral,
  entidades, casos de uso, adaptadores, frameworks & drivers, princípios, testes), cada um com o que
  a aula ensina, o que os autores dizem, como o projeto implementa, padrões, desvios conscientes e
  verificação no build. `package-info` das camadas e subpacotes principais resumidos com o link.
- Relatório: abertura, seção de escopo do Tech Challenge (restaurante, cardápio e CRUD de tipos
  pendentes), Visão Geral com o fluxo e o diagrama reais, Etapas 1, 3 e 4 alinhadas ao código,
  RFC 9457, geração do UUID, aulas e Freeman nas referências, seção da Etapa 14.
- Correções da revisão de código: "esqueci minha senha" aceito na requisição e processado na fila
  `forgotPasswordExecutor` (mesmo tempo de resposta exista ou não o e-mail); e-mail entregue só
  depois do commit (`MailOutbox`); timeouts de 5 s no SMTP (`SmtpTimeoutIT`); JWT em HS256 fixo;
  validade do e-mail com singular/plural e segundos; log de falha do SMTP com o assunto; escopo do
  relatório com a associação de tipo a usuários existentes como parcial; subseção "O que foi
  entregue" da etapa; índice de `docs/arquitetura/` alinhado; testes de ordenação dos papéis e de
  resposta sem esperar o envio; busca no Mailpit da coleção com nova tentativa.
- `mvn clean verify`: 458 testes unitários e 91 de integração; cobertura unitária 100% (1065
  linhas, 232 ramos). Newman: 52 requests, 108 asserções, nenhuma falha; e-mail no Mailpit idêntico.

## Etapa 15 — API REST em api/rest/spring, organizada como MVC
- `infrastructure/web` → `infrastructure/api/rest/spring` (papel, estilo e tecnologia, como os demais
  módulos), organizada por papel da classe: `controller`, `dto/request`, `dto/response`, `assembler`,
  `route`, `config`, `exception` (antes `error`), `doc`, `security`, `validation`. 53 classes movidas
  com `git mv`.
- `route/ApiRoutes` com os caminhos base: `@RequestMapping`, `SecurityConfig` e o assembler leem
  daqui. O assembler monta links com `BasicLinkBuilder.linkToCurrentMapping()`, sem depender do
  controller (evita o ciclo `controller` ↔ `assembler`).
- `UserWebMappingTest` dividido em `UserDtoMappingTest` e `UserModelAssemblerTest`.
- `InfrastructureModulesTest`: regras de `web` renomeadas para `api` e 7 regras novas de organização
  MVC (controllers, records `*Request`/`*Response`, `@RestControllerAdvice`, assemblers), conferidas
  com violações propositais — 23 regras.
- `mvn clean verify`: 465 testes unitários e 91 de integração; cobertura unitária 100% (1066
  linhas, 232 ramos). Newman: 52 requests, 108 asserções, nenhuma falha.

## Relatório v2.0 — Modelo de Dados v2 (planejamento)
- Documentos do novo modelo no projeto, como referência (fora de `db/migration`):
  `docs/modelo-dados/postech-2-restaurantes.sql` e `.pdf`, com `docs/modelo-dados/README.md`
  (escopo, divergências modelo → schema físico, observações sobre as tabelas de cardápio).
- `relatorios/relatorio-tech-challenge-fase02-v2.0.md` (a v1.0 fica como estava): histórico de
  versões; seção da Etapa 16 (restaurantes), que faltava, com as pendências frente às convenções;
  seção "Modelo de Dados v2 — adequação planejada" com o quadro atual × v2, diagrama ER, enums
  (`courier_vehicle_type`, `courier_status`, `day_of_week`), divergências e política de migração;
  Etapas 17 a 25 planejadas para `users`, `user_addresses`, `password_reset_tokens`, `owners`,
  `clients`, `couriers`, `admins`, `restaurants` e `restaurant_office_hours`.
- Ponto de atenção registrado no escopo: o modelo v2 fixa os tipos de usuário no schema, o que
  conflita com o requisito de CRUD do catálogo de tipos.
- Nenhuma mudança de código ou de migration.

## Etapa 17 — Reorganização dos pacotes do agregado de usuário
- Domínio: `Role`/`RoleName` em `domain/entity/role` e `PasswordResetToken` em
  `domain/entity/password`, com `package-info`; `user` passa a conter só `User`.
- Persistência: `persistence/jpa/user/{address,role,password}` espelhando o domínio, cada um com
  `package-info`. `AddressJpaEntity` deixa de referenciar `UserJpaEntity`: a associação vira
  `@OneToMany` + `@JoinColumn(nullable = false, updatable = false)` unidirecional, sem o ciclo
  `user` ↔ `user.address` que a regra `nenhum_ciclo_entre_pacotes` recusa (conferido ao contrário).
- Imports do módulo de restaurantes ajustados aos pacotes novos.
- Testes nos pacotes espelhados: `PasswordResetTokenTest`, `RoleTest`, `RoleNameTest` movidos;
  `JpaEntitiesTest` e `RoleAndTokenDataSourcesJpaTest` divididos em um teste por classe.
- Build destravado: `SchemaMigrationIT` e `OpenApiDocumentationIT` atualizados para V3 e os
  endpoints de restaurante (falhavam desde a Etapa 16); testes unitários que faltavam no módulo de
  restaurantes para voltar a 100% de ramos.
- `docs/arquitetura/01`, `04`, `05` e `CLAUDE.md` com a organização nova.
- `mvn clean verify`: 530 testes unitários e 92 de integração; cobertura unitária 100% (1346
  linhas, 268 ramos, 560 métodos). Newman: 67 requests, 124 asserções, nenhuma falha.

## Etapa 18 — Endereços via user_addresses e endereço próprio do restaurante
- Migration `V4__user_addresses.sql`: tabela `user_addresses` (rótulo, `is_default`, auditoria;
  `address_id` único; "um padrão por usuário" como restrição de exclusão adiada para o commit);
  endereços existentes viram vínculos (o de menor id é o padrão); cada restaurante ganha uma cópia
  própria do endereço que usava; `restaurants.address_id` único; `addresses.user_id` removida.
- Domínio: `UserAddress` (parte do agregado `User`), com "exatamente um padrão" verificado no
  `User`; `Restaurant` passa a ter um `Address` em vez de `addressId`.
- Aplicação: `UserAddressDTO` (promove o primeiro a padrão quando nenhum é marcado); DTOs de
  restaurante com `AddressDTO`; sai a regra "o endereço do restaurante é um dos endereços do dono".
- Adaptadores: `UserAddressData`, `UserAddressView`; `AddressMapping` (adapter/gateway/mapping) e
  `AddressPresenter`, compartilhados pelo usuário e pelo restaurante.
- Persistência: `AddressJpaEntity` em `persistence/jpa/address` (com `AddressJpaMapping`);
  `UserAddressJpaEntity` em `persistence/jpa/user/address`; restaurante com `@OneToOne` e
  `orphanRemoval` para o endereço; `@EntityGraph` carregando o endereço.
- API: endereços do usuário como `{ label, isDefault, address }`; restaurante com `address` no
  corpo e na resposta. Coleção Postman ajustada e prints regenerados.
- V4 verificada também sobre o volume do Compose com dados (restaurante usando o endereço do dono).
- Revisão de código: papéis carregados por subselect (o grafo papéis × endereços repetia endereços);
  `id` opcional no endereço do usuário mantém vínculo e endereço numa atualização (id alheio → 400);
  ITs contam só os endereços que criaram (`EnderecosNoBanco`), limpeza em `finally`; guarda de que a
  restrição adiada chega como 409.
- `mvn clean verify`: 560 testes unitários e 98 de integração; cobertura unitária 100% (1429
  linhas, 292 ramos, 597 métodos). Newman: 67 requests, 126 asserções, nenhuma falha.

## Etapa 19 — Token de redefinição único por usuário
- Migration `V5__one_reset_token_per_user.sql`: mantém só o token de validade mais distante de cada
  usuário (empate pelo id), cria `UNIQUE (user_id)` e remove o índice simples, redundante.
- Domínio: `PasswordResetToken.reissue(hash, validade, agora)` — hash e validade novos, uso zerado,
  tudo validado antes de mudar.
- Aplicação: `IPasswordResetTokenGateway.findByUserId`; `ForgotPasswordUseCase` reemite o token que
  o usuário já tinha (o link anterior deixa de valer) e só insere quando não há nenhum.
- Adaptadores e persistência: busca pelo dono; `update` passa a gravar hash, validade e uso.
- V5 verificada também sobre o volume do Compose com vários tokens do mesmo usuário.
- `mvn clean verify`: 565 testes unitários e 101 de integração; cobertura unitária 100% (1445
  linhas, 294 ramos, 600 métodos). Newman: 67 requests, 126 asserções, nenhuma falha.

## Etapa 20 — Perfis de usuário no domínio
- VOs `Cpf` e `Cnpj` (verificadores conferidos; CNPJ também no formato alfanumérico da Receita, em
  vigor desde julho de 2026), `Phone` (10 a 13 dígitos), `LicensePlate` (padrão antigo ou Mercosul)
  e `DriverLicense` (CNH, 11 dígitos).
- Enums do modelo de dados: `CourierVehicleType` (com `requiresLicense()`) e `CourierStatus`, ambos
  com `from(String)`.
- Entidades de perfil, sem id próprio (a identidade é a do usuário), em pacotes próprios:
  `OwnerProfile`, `ClientProfile` (nascimento não futuro, com o dia por parâmetro), `CourierProfile`
  (CNH e placa andam com o veículo; nasce `OFFLINE`) e `AdminProfile`.
- Ajuste do plano: as mudanças em `RoleName` (`ROLE_COURIER`, `ROLE_CUSTOMER` → `ROLE_CLIENT`) foram
  para a Etapa 21, junto com o fim do catálogo `roles`.
- Só o domínio mudou. `mvn clean verify`: 662 testes unitários e 101 de integração; cobertura
  unitária 100% (1600 linhas, 354 ramos, 666 métodos).

## Etapa 21 — Usuário composto por perfis (papel derivado)
- Migration `V6__user_profiles.sql`: tipos `courier_vehicle_type` e `courier_status`; tabelas
  `owners`, `clients`, `couriers` e `admins` (chave primária = usuário, `ON DELETE CASCADE`,
  auditoria), com o `CHECK` de CNH e placa por veículo; perfis dos usuários da seed; falha com
  mensagem se sobrar usuário sem perfil (não inventa CPF nem CNPJ); remove `roles` e `user_roles`.
- Domínio: `UserProfiles` (ao menos um perfil; CPF igual entre cliente e entregador; papéis
  derivados); `User` guarda os perfis; `RoleName` passa a `ROLE_OWNER`, `ROLE_CLIENT`,
  `ROLE_COURIER`, `ROLE_ADMIN`; sai a entidade `Role`.
- Aplicação: DTOs de perfil; `RegisterUserUseCase` recebe o relógio e recusa CPF e CNPJ já
  cadastrados (409); `IUserGateway` ganha `findByCpf` e `findByCnpj`; sai `IRoleGateway`. Criar ou
  alterar restaurante exige perfil de dono (administrador sem esse perfil recebe 403).
- Adaptadores e API: `OwnerData`, `ClientData`, `CourierData`, `AdminData`; views e respostas com
  os perfis e `roles` como lista de nomes; cadastro com os blocos `owner`, `client` e `courier` (o
  autocadastro não tem perfil de administrador); saem `RoleGateway`, `IRoleDataSource`, `RoleData`,
  `RoleView` e `RoleResponse`.
- Persistência: entidades JPA de cada perfil em `persistence/jpa/user/{owner,client,courier,admin}`,
  ligadas só do lado do usuário (sem ciclo de pacotes); `ENUM`s como texto com `@ColumnTransformer`;
  a exclusão tira e descarrega os perfis antes do usuário (o Hibernate apagaria o usuário primeiro e
  o `DELETE` do perfil falharia). Sai o pacote `persistence/jpa/user/role`.
- Testes: `Documentos` gera CPF, CNPJ e CNH válidos para os ITs; testes novos de perfis em todas as
  camadas. Postman: gerador de documentos no nível da coleção, cadastro de dono e de entregador,
  casos de CPF inválido, nenhum perfil, moto sem CNH, CPF e CNPJ duplicados e restaurante para quem
  não é dono; 76 requests e 150 asserções, sem falhas, em duas execuções; 76 prints.
- `mvn clean verify`: 680 testes unitários e 114 de integração; cobertura unitária 100% (1767
  linhas, 424 ramos, 737 métodos).

## Etapa 22 — Perfis em usuário existente e status do entregador
- Endpoints novos em `/api/v1/users/{id}/profiles`: `PUT owner`, `PUT client`, `PUT courier` (o
  próprio usuário ou um administrador), `PUT admin` (só administrador), `DELETE {tipo}` e
  `PATCH courier/status`. Os papéis do token valem a partir do próximo login.
- Domínio: `ProfileType`; `UserProfiles` ganha `withOwner`/`withClient`/`withCourier`/`withAdmin`,
  `without` e `has` (conjunto novo, mesmas regras: ao menos um perfil, o mesmo CPF para cliente e
  entregador); exceção `ResourceInUseException` (409).
- Aplicação: `SaveUserProfileUseCase` (entrada `UserProfileDTO`, interface selada; CPF e CNPJ de
  outro cadastro recusados; alterar o entregador mantém o status), `RemoveUserProfileUseCase` (perfil
  inexistente 404; último perfil 400; dono com restaurante 409, por `IRestaurantGateway.existsByUserId`)
  e `ChangeCourierStatusUseCase`; `AdminProfileDTO`.
- Adaptadores e infraestrutura: `UserController` recebe a origem de dados de restaurante;
  `existsByUserId` na porta, no gateway e na JPA de restaurante; `AdminProfileRequest`,
  `CourierStatusRequest`; handler da `ResourceInUseException`.
- Testes: unitários de cada caso de uso e das regras de `UserProfiles`; `UserProfilesApiIT` por HTTP;
  `OpenApiDocumentationIT` com as seis operações. Postman: pasta "Perfis" com o sucesso e cada erro
  documentado; 111 requests e 234 asserções, sem falhas, em duas execuções; 111 prints.
- `mvn clean verify`: 725 testes unitários e 122 de integração; cobertura unitária 100% (1877
  linhas, 474 ramos, 784 métodos).

### Etapa 22 — correções da revisão de código
- Autorização com os papéis do cadastro a cada requisição (`FindCurrentRolesUseCase`,
  `AuthController.currentRoles`, porta `ICurrentRolesReader` no filtro): perfil removido deixa de
  autorizar na hora, com o mesmo token.
- O último administrador não perde o perfil de administrador (`IUserGateway.countAdmins`).
- `ResourceInUseException` ganha categoria própria: 409 `recurso-em-uso`.
- O CPF é da pessoa: alterar o CPF do perfil de cliente corrige o do entregador e vice-versa.
- CPF e CNPJ únicos numa regra só (`application/policy/user/UniqueDocumentsPolicy`), consultando só o
  documento que mudou.
- Migration `V7__profile_integrity.sql`: gatilho que impede o mesmo CPF em dois usuários entre
  `clients` e `couriers` e chave estrangeira `restaurants.user_id → owners`. As migrations planejadas
  das Etapas 23 e 24 passam a V8 e V9.
- Postman: o 409 da remoção do perfil de dono usa o dono criado pela coleção, nunca a seed; 113
  requests, 236 asserções, sem falhas, em duas execuções; 113 prints.
- `mvn clean verify`: 740 testes unitários e 125 de integração; cobertura unitária 100% (1914
  linhas, 502 ramos, 795 métodos).
