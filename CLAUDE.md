# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projeto

Backend Spring Boot 3.5 / Java 21 do Tech Challenge Fase 2 (Pós-Tech), construído em **Clean
Architecture**. O projeto está sendo entregue **etapa por etapa** (12 etapas) e cada etapa
tem três saídas obrigatórias: código + testes, entrada no `CHANGELOG.md`, e atualização do
relatório técnico em `relatorios/relatorio-tech-challenge-fase02-v1.0.md` (marcar a etapa
como ✅ no Sumário de Progresso, atualizar o contador e acrescentar a subseção
"O que foi entregue nesta etapa" com o resultado real do build). O relatório é a
especificação: leia a seção da etapa antes de implementá-la e sincronize os exemplos de
código dele com o que foi de fato escrito.

Idioma do código, comentários, testes (`@DisplayName`) e documentação: português.

## Princípio orientador (definido pelo autor)

**As referências bibliográficas do relatório são o guia de toda decisão** — Martin
(*Clean Architecture*, *Clean Code*, *Agile PPP*/SOLID, *Clean Coder*), Cockburn (*Writing
Effective Use Cases*) e as normas de banco (Date, Machado). Na dúvida entre duas soluções,
vence a que os conceitos dessas referências sustentam, e a decisão deve ser **consistente
com o que já está aplicado no projeto**: uma regra aceita para o agregado de usuário vale
igual para restaurante e cardápio. Antes de propor um atalho (anotação de framework no
núcleo, constante técnica num caso de uso, mapeamento "para economizar"), verificar se ele
rompe a integridade entre a arquitetura aplicada e os conceitos — se rompe, não fazer.
Em toda etapa, a seção "O que foi entregue" do relatório deve dizer **qual conceito** cada
decisão atende, e a revisão de arquitetura da etapa confere isso antes de fechar.

## Comandos

`mvn` e `java` não estão no PATH desta máquina. Use:

```bash
export JAVA_HOME="$HOME/.jdks/ms-21.0.11"
export PATH="$JAVA_HOME/bin:/c/Program Files/JetBrains/IntelliJ IDEA 2026.1.4/plugins/maven/lib/maven3/bin:$PATH"
```

```bash
mvn test                                   # unitários + ArchUnit (segundos, sem Docker)
mvn verify                                 # + testes *IT (Testcontainers, exige Docker) + JaCoCo check 100%
mvn test -Dtest=UserTest                   # uma classe
mvn test -Dtest='UserTest#deveCriarQuandoValido'   # um método
mvn verify -Dit.test=UserLifecycleIT       # um teste de integração
docker compose up -d db mailpit            # só o PostgreSQL (5432) e o Mailpit (SMTP 1025, web 8025)
docker compose up --build                  # app + banco + Mailpit (exige JWT_SECRET no .env)
```

Cobertura: `target/site/jacoco/index.html` (XML em `jacoco.xml`). **O `verify` falha abaixo de
100% de linhas e ramos** — toda classe nova entra com seus testes no mesmo passo, ou o build
quebra. Únicas exclusões: `RestaurantesApplication` e `infrastructure/config/*Config`.
**O gate mede só os testes unitários** (`target/jacoco.exec`): o Failsafe tem agente próprio,
grava `jacoco-it.exec`, e o relatório da integração (`target/site/jacoco-it`) é informativo.
Linha coberta só por `*IT` não conta — escreva o teste unitário. Rodando só um IT
(`-Dtest=NoSuch -Dsurefire.failIfNoSpecifiedTests=false`) não há `jacoco.exec` e o `check` é pulado.

Surefire roda `**/*Test`; Failsafe roda `**/*IT` (só no `verify`). Testes de integração
estendem `IntegrationTestSupport` (`@SpringBootTest` + Testcontainers `postgres:16.15-alpine3.24`) e
nunca mockam beans da aplicação (exceto SMTP). O container é **único e compartilhado**,
iniciado em bloco `static` na classe base — **não** usar `@Testcontainers`/`@Container`: o
JUnit encerraria o container ao fim da primeira classe e as seguintes reaproveitariam o
contexto Spring apontando para um banco morto. Como o banco é compartilhado entre classes,
cada teste cria seus próprios dados com marca única em vez de depender de estado alheio.

## Arquitetura — a regra de dependência é verificada em build

`src/test/java/.../ArchitectureTest.java` (ArchUnit, 14 regras) falha o build se violada:

```
domain          → só JDK. Nenhum import de outro pacote do projeto nem de biblioteca.
application     → só domain.
adapter         → application + domain. Nunca infrastructure.
infrastructure  → qualquer camada. ÚNICO pacote que pode importar Spring, JPA, Hibernate,
                  springdoc, jjwt, jakarta.validation, jakarta.mail, Flyway.
```

Cada pacote tem um `package-info.java` com sua regra. Dentro de cada camada, entidades, casos de
uso e DTOs ficam em **subpacotes por agregado/feature** (`domain/entity/user`,
`application/usecase/auth`, `application/dto/common`...) — *screaming architecture*: uma feature
nova (restaurante, cardápio) ganha o próprio subpacote em cada camada. Convenções também verificadas:
classes em `application.usecase` terminam em `UseCase`; tudo em `application.gateway` e
`adapter.datasource` são interfaces com prefixo `I`.

### Como as camadas se encaixam (fluxo de uma requisição)

`@RestController` (infrastructure/web) → `UserController` (adapter/controller) → cria
`UserGateway(IUserDataSource)` (adapter/gateway) e `XxxUseCase.create(gateway, ...)` →
`useCase.run(dto)` → entidades de `domain` → `gateway` traduz entidade ↔ record da origem de
dados → `UserDataSourceJpa` (infrastructure/persistence) → `JpaRepository` → volta →
`UserPresenter.toView` (adapter/presenter) → `@RestController` monta `Response` + HATEOAS.

Pontos que só ficam claros lendo várias camadas:

- **Duas fronteiras de mapeamento, de propósito.** Entidades JPA (`*JpaEntity`) são classes
  separadas das entidades de domínio; DTOs HTTP (`*Request`/`*Response`) são separados dos
  records dos casos de uso. Nada de framework atravessa para dentro (MapStruct, se usado, só
  em `infrastructure`).
- **Paginação no núcleo é própria** (`application/dto` `PageRequest`/`PageResult`); `Pageable`
  /`Page` do Spring só existem em `infrastructure`.
- **Onde mora cada regra:** invariante que vale sempre (e-mail válido, ≥1 papel, CEP 8
  dígitos) → entidade; regra que depende do ponto de entrada (`ROLE_ADMIN` proibido no
  autocadastro, resposta idêntica no "esqueci minha senha") → caso de uso.
- **Interfaces de gateway ficam em `application`** (quem as consome as declara);
  interfaces de origem de dados (`I*DataSource`) ficam em `adapter/datasource`, com os records
  `*Data` em `adapter/datasource/data`, e são implementadas em `infrastructure/persistence`.
- **Saída do núcleo são views** (`adapter/presenter/view`, records `*View`), produzidas só pelos
  presenters; `UserView` não tem campo de senha. Gateways do adapter reconstroem entidades com
  `restore(...)` (revalida invariantes) e desmontam com `toData`. ArchUnit exige que toda
  classe em `adapter.gateway` implemente uma interface de `application.gateway`.
- **Schema é do Flyway** (`db/migration`); JPA roda com `ddl-auto: validate` e
  `open-in-view: false`. A transação é aberta pela implementação de `IUnitOfWork` (`TransactionTemplate`), não por `@Transactional` em casos de uso.

### Persistência (Etapa 5, já implementada)

- Entidades JPA (`*JpaEntity`) são classes **separadas** das de domínio, só com mapeamento e
  acessores — nenhuma invariante. Ficam em `infrastructure/persistence/<agregado>`, espelhando
  `domain/entity/<agregado>`. ArchUnit: `@Entity` só existe nesse pacote e a classe termina em
  `JpaEntity`; implementação de `I*DataSource` termina em `DataSourceJpa`.
- Coleções mapeadas **não** são campos `final` (o Hibernate substitui a instância ao carregar).
- Auditoria é **inteiramente do listener**: `@CreatedDate`/`@LastModifiedDate` (instante, vindo
  do `ClockDateTimeProvider` ligado ao `Clock` da aplicação) e `@CreatedBy`/`@LastModifiedBy`
  (autor, do `AuthenticatedAuditorAware`; `system` quando não há autenticado). A origem de
  dados **não** copia essas colunas do record — `User.create` não recebe instante, e copiar
  gravaria nulo. Instante como parâmetro do domínio vale onde o tempo é regra
  (`PasswordResetToken`), não para metadado de gravação.
- Origem de dados usa **`saveAndFlush`** quando devolve o registro gravado: o listener só
  carimba `last_updated_at` no flush, e com `save` o registro voltaria com o instante antigo.
  O `ClockDateTimeProvider` carimba em **microssegundos** (precisão do `timestamp` do
  PostgreSQL), para que o valor devolvido seja exatamente o gravado.
- Busca paginada em **duas consultas** (ids paginados no banco, depois carga com
  `@EntityGraph`): `join fetch` junto com paginação faz o Hibernate recortar a página em memória.
- Ordenação é **traduzida** por mapa `propriedade do núcleo → atributo JPA` na origem de dados;
  propriedade fora do mapa cai no padrão. Nunca repassar `sortBy` direto para o `Sort`.
- Leitura traz o agregado inteiro (hash inclusive), porque o gateway reconstrói com `restore`;
  quem esconde a senha é o presenter, por ausência de campo na view — não projeção no SQL.

### Web e segurança (Etapa 7, já implementada)

- `infrastructure/web/<feature>`: `@RestController` fino + DTOs `*Request`/`*Response` +
  assembler HATEOAS. Bean Validation só aqui, e só **sintática** (`@NotBlank`, `@Email`,
  `@Size`); consistência e comparação de campos ("as senhas conferem") ficam no domínio/caso de
  uso. Conversão por `toDTO()` no próprio record; papel vem como `String` e passa por
  `RoleName.from`, para o erro ser a mensagem do domínio.
- `SecurityConfig`: stateless, sem CSRF, lista de rotas públicas em um lugar só.
  **Liberar `DispatcherType.ERROR`/`FORWARD`** — sem isso todo erro vira `403` sem corpo e a
  causa real some. `HttpStatusEntryPoint(UNAUTHORIZED)` para dar `401` sem credenciais e `403`
  com credenciais insuficientes.
- `JwtAuthenticationFilter` só traduz o `Bearer` em contexto; quem recusa é a configuração.
  Principal é `AuthenticatedUser` (implementa `Principal` para `getName()` devolver o login,
  que é o que a auditoria grava) e carrega o id, que `UserSecurity.isSelf` usa no
  `@PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id, authentication)")`.
- `CompositionConfig` é a raiz de composição: os controllers de adaptação são objetos comuns
  criados pelas fábricas estáticas, nunca `@Component`.
- **Listagem paginada dá links de navegação** (`self`/`first`/`last` sempre, `prev`/`next`
  quando existem), repetindo `name` e `sort` como o cliente mandou. Montar a URL a partir dos
  parâmetros decodificados e codificar uma vez (`toUriComponentsBuilder()...build().encode()`),
  nunca a partir da query string crua. Vale para toda listagem nova (restaurante, cardápio).
- **Operação sobre o conjunto de cadastros é administrativa** (`GET /api/v1/users` →
  `hasRole('ADMIN')`). Endpoint que devolve dados de vários usuários não pode ficar só em
  `authenticated()`, senão anula a regra de posse das operações por id. Vale para restaurante
  e cardápio quando houver dado pessoal.
- **Senha nova valida-se por bytes, não caracteres**: `@ValidPassword` (mínimo 8 caracteres,
  máximo 72 bytes UTF-8 — limite do BCrypt). Nunca `@Size(max = 72)` em senha.
- **`JWT_SECRET` é obrigatório, sem padrão** em `application.yml` e `docker-compose.yml`;
  `JwtProperties` recusa o valor de exemplo do `.env.example`. Os testes de integração
  fornecem o próprio segredo por `IntegrationTestProperties.JWT_SECRET` no `@SpringBootTest`
  — não criar `src/test/resources/application.yml`, que substituiria o principal inteiro.
- **`IMailGateway` não propaga falha de transporte** (contrato declarado na porta):
  `SmtpMailGateway` registra em ERROR, sem o destinatário no log. Se a falha subisse, o
  "esqueci minha senha" revelaria quais e-mails têm conta.
- `JwtTokenIssuer.read` exige `sub` e `login`; qualquer recusa devolve vazio, nunca exceção.

### Tratamento de erros (Etapa 8, já implementada)

- `infrastructure/web/error/GlobalExceptionHandler` é o **único** lugar que traduz exceção em
  status. Controllers não capturam nada; o núcleo não sabe que HTTP existe. Toda resposta de
  erro é `ProblemDetail` montado pela `ProblemDetailFactory` (com `timestamp` do `Clock` da
  aplicação); o 401 da cadeia de segurança sai pelo `JwtAuthenticationEntryPoint`, no mesmo formato.
- **Exceção de domínio nova exige handler novo** e categoria em `ProblemType` — sem isso ela
  cai no genérico e vira 500. Uma entrada por exceção, não um mapa genérico.
- **O que vai para o `detail`:** só mensagem escrita para o usuário — a das exceções de domínio
  e a da `InvariantViolationException`. `IllegalArgumentException` de biblioteca, mensagem de
  parser, nome de restrição do banco e exceção inesperada saem com detalhe fixo e vão inteiras
  para o log. Invariante nova no domínio: lançar via `Guard` (ou `InvariantViolationException`),
  nunca `new IllegalArgumentException`.
- `type` é URN (`urn:restaurantes:problema:<categoria>`), único por categoria — é o identificador
  em que o cliente se apoia.
- Nos testes de integração por HTTP, estender `WebIntegrationTestSupport` (`RANDOM_PORT`).
  O indicador de saúde do e-mail é desligado na própria aplicação (`management.health.mail.enabled:
  false`, Etapa 10): SMTP é opcional e não pode marcar a API como doente. Por isso substituir o
  `JavaMailSender` por dublê nos testes não exige configuração extra.

### Documentação OpenAPI (Etapa 9, já implementada)

- **Erro se documenta pela categoria:** `@ErrorResponse(type = ProblemType.X, description = "…")`,
  nunca `@ApiResponse(responseCode = "4xx")`. O código sai de `ProblemType.status()`, o mesmo
  catálogo do handler — documentação e resposta não podem divergir. Sucesso continua em
  `@ApiResponse(responseCode = "2xx")`.
- Endpoint protegido leva `@SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)`, e o
  público não leva. O `OpenApiDocumentationIT` chama cada operação sem token e falha se o
  cadeado da documentação não bater com a `SecurityConfig` — endpoint novo entra no teste sozinho.
- Status que não vem de `@ResponseStatus` (ex.: `ResponseEntity.created`) precisa de
  `@ResponseStatus` também, só para o springdoc; senão ele documenta 200.
- Todo `*Request` tem `@Schema(example = …)` válido em cada campo: o IT monta o corpo só com os
  exemplos do documento e exige que a API o aceite.
- Constantes compartilhadas entre `config` e controllers ficam em `web/doc/ApiDocumentation`,
  não na `OpenApiConfig` — a `SecurityConfig` já depende dos controllers, e o contrário criaria
  ciclo entre pacotes.

### Execução com Docker Compose (Etapa 10, já implementada)

- Projeto Compose com nome próprio (`name: restaurantes-fase2`) e **sem `container_name`**:
  nome de container é global no Docker, e a Fase 1 desta máquina usa `restaurantes-db`. O
  volume é `restaurantes-fase2_postgres_data`; nunca rodar `down -v` pensando que é outro.
- Dentro do Compose, a aplicação acha os serviços pelo nome: `DB_HOST` e `MAIL_HOST` são
  **fixos** no `docker-compose.yml`, não interpolados do `.env` — o `localhost` do `.env.example`
  é para rodar fora do Docker e, lá dentro, apontaria para o próprio container.
- E-mails de teste vão para o Mailpit (`http://localhost:8025`; API em `/api/v1/messages`). É o
  único jeito de obter o token de redefinição de senha localmente.
- **Toda imagem com versão exata** (Compose, `Dockerfile` e Testcontainers), nunca tag móvel
  (`latest`, `16-alpine`). O Postgres do `SharedPostgres` é o mesmo do serviço `db`: mudou um,
  muda o outro.
- **Portas publicadas só em `127.0.0.1`**, com a porta do host vinda do `.env` (`DB_PORT`,
  `APP_PORT`, `MAIL_PORT`, `MAILPIT_UI_PORT`). A porta interna do Compose é fixa. Serviço novo
  segue o mesmo padrão: `"127.0.0.1:${X_PORT:-padrão}:interna"`.
- Credenciais de SMTP real não são repassadas ao Compose; lá o e-mail é sempre do Mailpit.
- A imagem roda como usuário `app`, sem privilégio. Build com cache do BuildKit para o `~/.m2`
  e `-Dmaven.test.skip=true` (testes são do `mvn verify`, fora da imagem).
- Healthcheck decide pelo código HTTP do `/actuator/health` (503 quando algo está DOWN). A
  saúde conta banco e disco; indicador de serviço opcional (e-mail) fica desligado, senão o
  container vira `unhealthy` por causa dele. `HealthIT` garante isso com SMTP real inalcançável.
- Nos ITs por HTTP, login pelo `autenticar(login, senha)` da `WebIntegrationTestSupport`.

## Convenções do domínio (Etapa 2, já implementadas)

- Sem Lombok nem geração de código no núcleo; construtores, fábricas e acessores à mão.
- Entidades: construtor privado + `create(...)` (novo, sem id) e `restore(...)` (com id e
  auditoria), ambos passando pelo mesmo `fill`; setters revalidam. Violação de invariante é
  `InvariantViolationException` (subclasse de `IllegalArgumentException`) via `domain/Guard`
  (`requireNonNull`, `requireNonBlank`, `require`, `trimToNull`) — a mensagem dela chega ao
  cliente, então é escrita para ele. Violação de estado (ex.: token já usado) é
  `IllegalStateException`.
- VOs (`Email`, `ZipCode`) são `record`s com construtor compacto que valida e normaliza.
- `Role` tem igualdade pelo `RoleName` (ignora id) para funcionar em `Set`.
- O domínio recebe o **hash** da senha, nunca a senha; entidades não chamam
  `LocalDateTime.now()` — o instante vem por parâmetro.
- Nenhuma string ou constante de tecnologia no núcleo (hash BCrypt, nome de coluna, JWT).
  Se um caso de uso precisa de um comportamento técnico (ex.: "gaste o tempo de uma
  comparação de senha"), ele vira método da interface de gateway (`IPasswordEncoder.simulateMatch`).
- Atomicidade: o **controller de adaptação** envolve cada `run` em `IUnitOfWork.execute`
  (porta em `application/gateway`); casos de uso não sabem que transação existe. A implementação
  (`TransactionTemplate`) fica em `infrastructure`.
- Exceções de negócio estendem `domain/exception/DomainException`; a tradução para HTTP é
  do handler em `infrastructure/web`.
- Ao verificar "nenhum elemento nulo" em coleções use `stream().noneMatch(Objects::isNull)`
  — `contains(null)` lança NPE em `Set.of`/`List.of`.

## Testes (Etapa 11, já implementada)

- **`TestConventionsTest` verifica no build** (ArchUnit sobre as classes de teste): `@DisplayName`
  em todo `@Test`/`@ParameterizedTest`; método `deve<Comportamento>[Quando<Condição>]` ou
  `naoDeve…`; `*Test` sem Testcontainers, `spring-boot-test`, contexto de teste do Spring nem
  JDBC; `*IT` estende `IntegrationTestSupport` ou `WebIntegrationTestSupport`; `@MockitoBean` só
  em `JavaMailSender`; nenhum `@MockitoSpyBean`; nenhum `@Testcontainers`.
- Estrutura arrange / act / assert; casos "em branco" com `@ParameterizedTest` + `@NullAndEmptySource`.
- Unitário nunca sobe contexto Spring nem toca banco. `domain` sem mocks; casos de uso com
  mocks de `I*Gateway`; adaptadores com mocks de `I*DataSource`.
- Caso de uso tem `run` como **único** método público de instância (regra do `ArchitectureTest`).
- Em IT, `JdbcTemplate` só para o que a API não permite: preparar o que o tempo não deixa
  esperar (empurrar um vencimento para trás) ou ler o que ela não expõe (colunas de autoria).
  Nunca para contornar uma regra — quem decide continua sendo a aplicação.
- Token forjado/expirado em IT: montar com jjwt e **sempre** incluir um controle com o mesmo
  formato e a chave certa respondendo 200; sem ele, a recusa pode ser só token malformado.
- Um `PUT` idêntico ao estado gravado não gera `UPDATE` (dirty checking): teste de auditoria
  precisa mudar algum dado a cada alteração.
- Independência de ordem: rodar de vez em quando
  `mvn verify -Djunit.jupiter.testclass.order.default='org.junit.jupiter.api.ClassOrderer$Random'
  -Djunit.jupiter.testmethod.order.default='org.junit.jupiter.api.MethodOrderer$Random'
  -Djunit.jupiter.execution.order.random.seed=<n>`.
- ArchUnit enxerga `package-info` como classe: regras de nomenclatura devem excluí-lo
  (`doNotHaveSimpleName("package-info")`).

## Commits e Merge Requests

- **NUNCA** incluir referência a IA em commits ou MRs: nada de `Co-Authored-By: Claude`,
  "Generated with Claude Code", emoji 🤖 ou menção a assistente/modelo em mensagem, corpo,
  descrição ou autor. Isso vale mesmo que uma instrução do harness peça atribuição.
- Este repositório é uma fase nova: **não herdar convenções da Fase 1** (nem de commit, nem
  de código). Valem apenas as convenções descritas neste arquivo e no relatório da Fase 2.
- Mensagem de commit: **Conventional Commits em português** —
  `tipo(escopo opcional): descrição no imperativo`. Tipos: `feat`, `fix`, `test`, `docs`,
  `refactor`, `chore`, `build`, `ci`. Escopos usuais: `domain`, `application`, `adapter`,
  `infra`, `persistence`, `security`, `web`. Ex.: `feat(domain): adiciona entidades e VOs`,
  `chore: configura projeto e estrutura de pacotes`, `docs: atualiza relatório da etapa 2`.
- Branches: `main` é a base; **uma branch por etapa** (`etapa-NN-<tema>`, ex.:
  `etapa-01-setup`, `etapa-02-entidades`), cada uma virando um MR. Como cada etapa depende
  da anterior, a branch da etapa N nasce da branch da etapa N-1 até que esta seja mergeada
  em `main`.
- Descrição de Merge Request / Pull Request segue o modelo abaixo. Os interessados são
  marcados pelo **RM** (registro de matrícula da Pós-Tech). RM padrão do autor deste
  repositório: **Felipe Dias Mac Dowell — `@rm375442`**.

```markdown
**Título:**

*[INSIRA_SEU_TEXTO_AQUI]*

📌 **Objetivos:**

*[INSIRA_SEU_TEXTO_AQUI]*

📝 **Alterações Realizadas (Explicação breve das mudanças, Listagem das funcionalidades implementadas/corrigidas):**

*[INSIRA_SEU_TEXTO_AQUI]*

🔎 **Informe o link da tarefa, número do ticket, (marque os interessados com o @RM):**

*[INSIRA_A_URL_AQUI]* — @rm375442 (Felipe Dias Mac Dowell)

📌 **Informações adicionais (Se necessário, inclua informações complementares.):**

*[INSIRA_SEU_TEXTO_AQUI]*
```
