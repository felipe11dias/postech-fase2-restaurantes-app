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
docker compose up -d db                    # só o PostgreSQL (porta 5432)
docker compose up --build                  # app + banco
```

Cobertura: `target/site/jacoco/index.html` (XML em `jacoco.xml`). **O `verify` falha abaixo de
100% de linhas e ramos** — toda classe nova entra com seus testes no mesmo passo, ou o build
quebra. Únicas exclusões: `RestaurantesApplication` e `infrastructure/config/*Config`.

Surefire roda `**/*Test`; Failsafe roda `**/*IT` (só no `verify`). Testes de integração
usam `@SpringBootTest` + Testcontainers `postgres:16-alpine` e nunca mockam beans da
aplicação (exceto SMTP).

## Arquitetura — a regra de dependência é verificada em build

`src/test/java/.../ArchitectureTest.java` (ArchUnit, 8 regras) falha o build se violada:

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
`UserPresenter.toDTO` (adapter/presenter) → `@RestController` monta `Response` + HATEOAS.

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
  interfaces de origem de dados (`I*DataSource`) ficam em `adapter` e são implementadas em
  `infrastructure/persistence`.
- **Schema é do Flyway** (`db/migration`); JPA roda com `ddl-auto: validate` e
  `open-in-view: false`. `@Transactional` só em `*DataSourceJpa`.

## Convenções do domínio (Etapa 2, já implementadas)

- Sem Lombok nem geração de código no núcleo; construtores, fábricas e acessores à mão.
- Entidades: construtor privado + `create(...)` (novo, sem id) e `restore(...)` (com id e
  auditoria), ambos passando pelo mesmo `fill`; setters revalidam. Violação de invariante é
  `IllegalArgumentException` via `domain/Guard` (`requireNonNull`, `requireNonBlank`,
  `require`, `trimToNull`). Violação de estado (ex.: token já usado) é `IllegalStateException`.
- VOs (`Email`, `ZipCode`) são `record`s com construtor compacto que valida e normaliza.
- `Role` tem igualdade pelo `RoleName` (ignora id) para funcionar em `Set`.
- O domínio recebe o **hash** da senha, nunca a senha; entidades não chamam
  `LocalDateTime.now()` — o instante vem por parâmetro.
- Nenhuma string ou constante de tecnologia no núcleo (hash BCrypt, nome de coluna, JWT).
  Se um caso de uso precisa de um comportamento técnico (ex.: "gaste o tempo de uma
  comparação de senha"), ele vira método da interface de gateway (`IPasswordEncoder.simulateMatch`).
- Casos de uso com mais de uma escrita ainda não são atômicos (`@Transactional` só no data
  source). Decisão pendente para a Etapa 4: porta `IUnitOfWork` usada pelo controller de adaptação.
- Exceções de negócio estendem `domain/exception/DomainException`; a tradução para HTTP é
  do handler em `infrastructure/web`.
- Ao verificar "nenhum elemento nulo" em coleções use `stream().noneMatch(Objects::isNull)`
  — `contains(null)` lança NPE em `Set.of`/`List.of`.

## Testes

- Estrutura arrange / act / assert; `@DisplayName` em linguagem de negócio; métodos
  `deve<Comportamento>Quando<Condição>`; casos "em branco" com `@ParameterizedTest` +
  `@NullAndEmptySource`.
- Unitário nunca sobe contexto Spring nem toca banco. `domain` sem mocks; casos de uso com
  mocks de `I*Gateway`; adaptadores com mocks de `I*DataSource`.
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
