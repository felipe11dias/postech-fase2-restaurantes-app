# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projeto

Backend Spring Boot 3.5 / Java 21 do Tech Challenge Fase 2 (Pós-Tech), construído em **Clean
Architecture**. O projeto está sendo entregue **etapa por etapa** (22 até aqui; as Etapas 23
a 25, de adequação ao Modelo de Dados v2 em `docs/modelo-dados/`, estão planejadas no
relatório) e cada etapa tem três saídas obrigatórias: código + testes, entrada no
`CHANGELOG.md`, e atualização do relatório técnico em `relatorios/relatorio-tech-challenge-fase02-v2.0.md` (marcar a etapa
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
quebra. Únicas exclusões: `RestaurantesApplication` e `infrastructure/**/*Config` (a regra
`configuracao_tem_nome_e_anotacao` garante que `*Config` é só `@Configuration`).
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

`src/test/java/.../ArchitectureTest.java` (ArchUnit, 14 regras) falha o build se violada — e o
`InfrastructureModulesTest` (23 regras, Etapas 13 a 15) faz o mesmo *dentro* da infraestrutura:

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
classes em `application.usecase` terminam em `UseCase`; tudo em `application.gateway`,
`adapter.datasource` e `adapter.service` são interfaces com prefixo `I`.

**Cada camada tem um documento em `docs/arquitetura/`** (aula × autor × código, padrões, desvios
conscientes, regra que verifica). **Decisão ou padrão novo atualiza o documento correspondente em
`docs/arquitetura/`** no mesmo passo — e o `package-info` da camada, se o resumo mudar.

### Infraestrutura em módulos substituíveis (Etapa 13, já implementada)

Cada subpacote de `infrastructure` é um **módulo-plugin**: o pacote diz o papel, o subpacote a
tecnologia. Trocar uma tecnologia = apagar um subpacote e criar outro ao lado.

```
infrastructure/
  main/                composição (Main): CompositionConfig, PasswordResetProperties
  api/rest/spring/     entrega HTTP: API REST em Spring, organizada como MVC (Etapa 15)
    controller/        @RestController (*RestController)
    dto/request/       records *Request (Bean Validation sintática, toDTO())
    dto/response/      records *Response
    assembler/         assemblers HATEOAS (*Assembler)
    route/             ApiRoutes — caminhos base (/api/v1/users, /api/v1/auth)
    config/            ForgotPasswordConfig (fila do "esqueci minha senha")
    exception/         GlobalExceptionHandler, ProblemDetailFactory, ProblemType
    doc/               OpenApiConfig, ApiDocumentation, @ErrorResponse, customizers
    validation/        @ValidPassword
    security/          SecurityConfig, BearerTokenAuthenticationFilter, 401, AuthenticatedUser,
                       UserSecurity, AuthenticatedActor, IAccessTokenReader e ICurrentRolesReader (portas do módulo)
  persistence/jpa/     PersistenceConfig, TransactionalUnitOfWork; audit/; um subpacote por agregado
  token/jwt/           ITokenEncoder + IAccessTokenReader (jjwt): JwtTokenEncoder, JwtProperties, JwtConfig
  crypto/              IPasswordEncoder (BCrypt), ISecureTokenGenerator (SecureRandom)
  mail/smtp/           IMailSender (Spring Mail): SmtpMailSender, MailProperties, MailConfig
```

Regras (verificadas pelo `InfrastructureModulesTest`):
- **Nenhum ciclo entre pacotes no projeto inteiro** (ADP). Entidade JPA de parte de um agregado
  fica num subpacote do agregado (`persistence/jpa/user/{address,password,owner,client,courier,admin}`; `user/address` é
  o vínculo `user_addresses`), e a dependência só vai do agregado para a parte: a parte não referencia
  a raiz (`@OneToMany` + `@JoinColumn` unidirecional do lado do `UserJpaEntity`), senão os dois
  pacotes formam ciclo. Entidade compartilhada por agregados fica em pacote próprio que não conhece
  nenhum deles: o endereço está em `persistence/jpa/address` (com `AddressJpaMapping`), usado pelo
  usuário e pelo restaurante.
- **Nenhum módulo conhece outro módulo-irmão; só `main` liga as pontas.** Quando um módulo precisa
  de algo de outro, ele declara a interface (ou recebe um `Supplier`) e o `main` liga. Ex.: o autor
  da auditoria vem de `api/rest/spring/security/AuthenticatedActor` para `persistence/jpa/audit` via
  `CompositionConfig`. Única exceção prevista: `token` implementa `IAccessTokenReader` e devolve
  `AuthenticatedUser`, as duas classes de `api/rest/spring/security` que ele pode conhecer.
- **Cada biblioteca só no seu módulo:** JPA/Hibernate/Spring Data/transação em `persistence` (e
  `main`, para ligar o `AuditorAware`); jjwt em `token.jwt`; Spring Mail em `mail.smtp`; Spring
  Security em `api` (e `spring-security-crypto` em `crypto`); Spring MVC, HATEOAS, Servlet, Bean
  Validation e springdoc em `api`.
- **A infraestrutura só conhece, do núcleo, as portas técnicas** (`IPasswordEncoder`,
  `ISecureTokenGenerator`, `IUnitOfWork`) — regra `infraestrutura_so_conhece_portas_tecnicas`. A
  regra proíbe *depender*, não só implementar: uma lambda num `@Bean` escaparia de `implement(...)`.
  Toda outra porta de `application.gateway` é implementada por um gateway em `adapter/gateway`,
  que consome a infraestrutura por uma interface de `adapter/datasource` ou `adapter/service`.
  `mail` e `token` não conhecem o `domain` (`transporte_nao_conhece_o_dominio`): só transportam e
  codificam o que o gateway traduziu.
- **Cada módulo habilita a própria configuração** (`JwtConfig`, `MailConfig`, `PersistenceConfig`,
  `SecurityConfig`, `OpenApiConfig`); `CompositionConfig` não conhece propriedade de tecnologia.
- Política da aplicação não mora em módulo de tecnologia: a validade do token de redefinição é
  `main/PasswordResetProperties` (`PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES`), e chega ao e-mail
  pela porta `IMailGateway.sendPasswordReset(to, token, validity)`; quem escreve o texto é o
  `PasswordResetMailGateway` (adaptador).
- Módulo novo (ex.: armazenamento da foto do prato): pacote de papel + subpacote de tecnologia,
  `package-info` dizendo qual porta implementa e como substituí-lo, e as regras acima estendidas.

### Como as camadas se encaixam (fluxo de uma requisição)

`@RestController` (infrastructure/api/rest/spring/controller) → `UserController` (adapter/controller) → cria
`UserGateway(IUserDataSource)` (adapter/gateway) e `XxxUseCase.create(gateway, ...)` →
`useCase.run(dto)` → entidades de `domain` → `gateway` traduz entidade ↔ record da origem de
dados → `UserDataSourceJpa` (infrastructure/persistence/jpa) → `JpaRepository` → volta →
`UserPresenter.toView` (adapter/presenter) → `@RestController` monta `Response` + HATEOAS.

Pontos que só ficam claros lendo várias camadas:

- **Duas fronteiras de mapeamento, de propósito.** Entidades JPA (`*JpaEntity`) são classes
  separadas das entidades de domínio; DTOs HTTP (`*Request`/`*Response`) são separados dos
  records dos casos de uso. Nada de framework atravessa para dentro (MapStruct, se usado, só
  em `infrastructure`).
- **Paginação no núcleo é própria** (`application/dto` `PageRequest`/`PageResult`); `Pageable`
  /`Page` do Spring só existem em `infrastructure`.
- **Onde mora cada regra:** invariante que vale sempre (e-mail válido, ≥1 perfil, CPF igual
  entre cliente e entregador, CEP 8 dígitos) → entidade; regra que depende do ponto de entrada
  (perfil de administrador fora do autocadastro, CPF/CNPJ únicos, resposta idêntica no "esqueci
  minha senha") → caso de uso.
- **Interfaces de gateway ficam em `application`** (quem as consome as declara);
  interfaces de origem de dados (`I*DataSource`) ficam em `adapter/datasource`, com os records
  `*Data` em `adapter/datasource/data`, e são implementadas em `infrastructure/persistence/jpa`.
- **Gateways de serviço (Etapa 14).** Serviço externo que não é origem de dados segue o mesmo
  formato: interface em `adapter/service` (`IMailSender`, `ITokenEncoder`; records em
  `adapter/service/data`, ex.: `TokenClaimsData`), gateway que **traduz** em `adapter/gateway`
  (`PasswordResetMailGateway` monta assunto e corpo; `TokenGateway` faz `User` → claims) e
  implementação que só transporta/codifica na infraestrutura (`SmtpMailSender`, `JwtTokenEncoder`,
  sem import do domínio). O controller recebe o serviço e cria o gateway **a cada operação**.
  Critério: **havendo tradução, há gateway**; porta sem tradução (hash, aleatório, transação) é
  técnica e vai direto — e entrar nessa lista exige justificativa no relatório.
- **Saída do núcleo são views** (`adapter/presenter/view`, records `*View`), produzidas só pelos
  presenters; `UserView` não tem campo de senha. Gateways do adapter reconstroem entidades com
  `restore(...)` (revalida invariantes) e desmontam com `toData`. ArchUnit exige que toda
  classe em `adapter.gateway` implemente uma interface de `application.gateway`. Tradução de
  parte compartilhada por agregados (o endereço) fica em `adapter/gateway/mapping` (`AddressMapping`)
  e, na saída, em `AddressPresenter` — uma tradução só, usada pelos dois gateways/presenters.
- **Schema é do Flyway** (`db/migration`); JPA roda com `ddl-auto: validate` e
  `open-in-view: false`. A transação é aberta pela implementação de `IUnitOfWork` (`TransactionTemplate`), não por `@Transactional` em casos de uso.

### Persistência (Etapa 5, já implementada)

- Entidades JPA (`*JpaEntity`) são classes **separadas** das de domínio, só com mapeamento e
  acessores — nenhuma invariante. Ficam em `infrastructure/persistence/jpa/<agregado>`, espelhando
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
- **Endereço (Etapa 18).** O usuário se liga aos endereços por `user_addresses` (rótulo, `is_default`);
  o restaurante tem `address_id` próprio e único. As chaves estrangeiras apontam **para**
  `addresses`, então o `CASCADE` do banco não remove o endereço: quem remove é o `orphanRemoval` da
  parte do agregado (`@OneToOne(cascade = ALL, orphanRemoval = true)`). Nos ITs, conferir que não
  sobra endereço — contando só os ids que o próprio teste criou (`EnderecosNoBanco.existentes`), nunca
  a tabela inteira. Endereço é atualizado na mesma linha: o do restaurante sempre; o do usuário
  quando o pedido traz o `id` dele (sem `id`, é novo; os ausentes saem). O `User` recusa id que não é
  seu.
- **"Um padrão por usuário" é restrição adiada** (`EXCLUDE ... WHERE (is_default) DEFERRABLE
  INITIALLY DEFERRED`): ao trocar a lista, o Hibernate insere os vínculos novos antes de apagar os
  antigos, e um índice único parcial recusaria essa troca válida. Restrição nova que o Hibernate
  pode violar no meio da descarga segue o mesmo desenho.
- **Nunca buscar um `Set` e uma `List` (bag) no mesmo `@EntityGraph`**: o produto cartesiano repete
  os itens da lista. Os perfis são `@OneToOne` (não repetem linhas) e entram no grafo com os endereços.
- **Perfis do usuário (Etapa 21, V6).** O papel é derivado dos perfis (`owners`, `clients`, `couriers`,
  `admins`) e não é gravado; não há `roles` nem `user_roles`. Os perfis JPA ficam em
  `persistence/jpa/user/{owner,client,courier,admin}`, ligados só do lado do usuário (`@OneToOne(cascade =
  ALL, orphanRemoval = true)` + `@PrimaryKeyJoinColumn`; perfil sem referência ao usuário, senão há
  ciclo), com `@Id` atribuído pela origem de dados (`ProfileJpaMapping`). Por isso: no cadastro, o
  usuário é gravado (`save`) antes dos perfis; na exclusão, os perfis saem e há `flush` **antes** de
  apagar o usuário — o Hibernate apagaria o usuário primeiro, o `ON DELETE CASCADE` levaria o perfil e o
  `DELETE` do perfil falharia (`StaleObjectStateException`). `ENUM` do PostgreSQL é `String` na
  entidade JPA com `columnDefinition` e `@ColumnTransformer(write = "?::tipo")`; o gateway converte
  (`CourierVehicleType.from`). Nos ITs, CPF, CNPJ e CNH vêm de `Documentos` (únicos, válidos); no HTTP,
  `perfilDeCliente()`/`perfilDeDono()` da `WebIntegrationTestSupport`.
- **Perfis de cadastro existente (Etapa 22).** Incluir ou alterar é `PUT /users/{id}/profiles/{owner,client,courier,admin}`
  (o de admin só por administrador, no `@PreAuthorize`); remover, `DELETE /profiles/{tipo}`; status do
  entregador, `PATCH /profiles/courier/status`. Um caso de uso para incluir/alterar (`SaveUserProfileUseCase`,
  entrada `UserProfileDTO` selada): perfil novo de tipo novo exige um `case` lá, e o compilador cobra. Regras
  entre perfis ficam em `UserProfiles` (`with*`, `without`, `has` devolvem conjunto novo e revalidam). A autorização
  usa os papéis **do cadastro, a cada requisição** (`BearerTokenAuthenticationFilter` troca os do token pelos de
  `ICurrentRolesReader`, ligado em `main` ao `AuthController.currentRoles`): perfil incluído ou removido vale na
  hora, com o mesmo token. Recurso que não pode sair porque outro depende dele (dono com restaurante, último
  administrador) é `ResourceInUseException` → 409 `recurso-em-uso` (categoria própria). CPF e CNPJ únicos: uma
  regra só, `application/policy/user/UniqueDocumentsPolicy` (consulta só o documento que mudou); regra de
  aplicação compartilhada por casos de uso vai para `application/policy/<agregado>`, não copiada. O CPF é da
  pessoa: alterar o de um perfil corrige o do outro (`UserProfiles.withClient`/`withCourier`).
- **Integridade dos perfis no banco (V7).** Gatilho `cpf_de_uma_so_pessoa` (trava consultiva por CPF) impede o
  mesmo CPF em dois usuários entre `clients` e `couriers`; `restaurants.user_id → owners` impede restaurante sem
  perfil de dono. Regra que a aplicação confere antes de gravar e que duas requisições simultâneas podem furar
  ganha garantia no schema. Na Etapa 23, a cascata de `restaurants.user_id` vale também para `fk_restaurants_owner`.

### API REST organizada como MVC (Etapa 15, já implementada)

- A API mora em `infrastructure/api/rest/spring`: papel (`api`), estilo (`rest`) e tecnologia
  (`spring`), como os outros módulos. Dentro dele, **um pacote por papel da classe**, não por
  feature: `controller`, `dto/request`, `dto/response`, `assembler`, `route`, `config`,
  `exception`, `doc`, `security`, `validation`. Feature nova na API (restaurante, cardápio) =
  classes novas **nesses mesmos pacotes**; nenhum subpacote por feature aqui. A *screaming
  architecture* continua valendo em `domain`, `application` e `adapter` (desvio consciente
  registrado no relatório e em `docs/arquitetura/04-frameworks-drivers.md`).
- Verificado pelo `InfrastructureModulesTest`: `@RestController` só em `controller` e com sufixo
  `RestController` (e `controller` só tem isso); `dto/request` só records `*Request`,
  `dto/response` só records `*Response`, e todo record `*Request`/`*Response` da API mora lá;
  `@RestControllerAdvice` só em `exception`; `assembler` só `*Assembler`.
- **Caminhos base ficam em `route/ApiRoutes`**, nunca no controller: `@RequestMapping`,
  `SecurityConfig` e os links do assembler leem de lá. O assembler monta links com
  `BasicLinkBuilder.linkToCurrentMapping().slash(ApiRoutes.X)` — se usasse
  `linkTo(XRestController.class)`, `controller` e `assembler` formariam ciclo (o controller usa o
  assembler) e `nenhum_ciclo_entre_pacotes` quebraria o build.

### Web e segurança (Etapa 7, já implementada)

- `controller` + `dto/request`/`dto/response` + `assembler`: `@RestController` fino, DTOs e
  assembler HATEOAS. Bean Validation só aqui, e só **sintática** (`@NotBlank`, `@Email`,
  `@Size`); consistência e comparação de campos ("as senhas conferem") ficam no domínio/caso de
  uso. Conversão por `toDTO()` no próprio record; enum (tipo de veículo) vem como `String` e passa
  por `CourierVehicleType.from`, para o erro ser a mensagem do domínio.
- `SecurityConfig`: stateless, sem CSRF, lista de rotas públicas em um lugar só.
  **Liberar `DispatcherType.ERROR`/`FORWARD`** — sem isso todo erro vira `403` sem corpo e a
  causa real some. `ProblemDetailAuthenticationEntryPoint` dá `401` sem credenciais e o
  `@PreAuthorize`, `403` com credenciais insuficientes.
- `BearerTokenAuthenticationFilter` só traduz o `Bearer` em contexto (lendo pela porta
  `IAccessTokenReader`, sem saber que é JWT); quem recusa é a configuração.
  Principal é `AuthenticatedUser` (implementa `Principal` para `getName()` devolver o login,
  que é o que a auditoria grava) e carrega o id, que `UserSecurity.isSelf` usa no
  `@PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id, authentication)")`.
- `main/CompositionConfig` é a raiz de composição: os controllers de adaptação são objetos comuns
  criados pelas fábricas estáticas, nunca `@Component`. Feature nova ganha os seus `@Bean` aqui.
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
- **O envio de e-mail não propaga falha de transporte** (contrato declarado em `IMailGateway` e
  repassado a `IMailSender`): `SmtpMailSender` registra em ERROR com o assunto, sem o destinatário
  no log. Se a falha subisse, o "esqueci minha senha" revelaria quais e-mails têm conta. O SMTP tem
  **timeouts** de 5 s (`spring.mail.properties.mail.smtp.*timeout`, conferidos pelo `SmtpTimeoutIT`):
  sem eles o Jakarta Mail espera para sempre.
- **O "esqueci minha senha" responde no mesmo tempo, exista ou não o e-mail.** O
  `AuthRestController` só valida a sintaxe e entrega o pedido à fila `forgotPasswordExecutor`
  (`api/rest/spring/config/ForgotPasswordConfig`: 2 threads, fila de 100, excedente descartado com aviso); o
  processamento — busca, gravação do token, e-mail — acontece fora da requisição. Esperar o envio
  ou a gravação faria a latência revelar quem tem conta. Endpoint novo com a mesma natureza
  (resposta que não pode revelar existência) segue o mesmo desenho.
- **E-mail sai depois do commit.** O `AuthController` entrega ao caso de uso uma `MailOutbox`
  (adapter/controller) e só chama `deliver()` depois que `unitOfWork.execute` retorna: se o commit
  falhar, nenhum e-mail com token inexistente sai, e a transação não espera o servidor de e-mail.
- **Um token de redefinição por usuário (Etapa 19, V5: `password_reset_tokens.user_id` único).** O
  `ForgotPasswordUseCase` reemite o token existente (`PasswordResetToken.reissue`) em vez de inserir
  outro: o link do e-mail anterior para de funcionar. A origem de dados grava hash, validade e uso no
  `update`. Dois pedidos simultâneos para o mesmo e-mail: um vence, o outro viola a unicidade dentro
  da fila e fica no log (ERROR), sem mudar a resposta 202. Em IT com dois pedidos, `reset(mailSender)`
  entre eles: `tokenEnviado()` exige exatamente um envio.
- O JWT é assinado em **HS256 fixo** (`signWith(key, Jwts.SIG.HS256)`); deixado ao jjwt, o algoritmo
  seria escolhido pelo tamanho do segredo.
- `JwtTokenEncoder.read` (implementa `IAccessTokenReader`) exige `sub` e `login`; qualquer recusa devolve vazio, nunca exceção.

### Tratamento de erros (Etapa 8, já implementada)

- `infrastructure/api/rest/spring/exception/GlobalExceptionHandler` é o **único** lugar que traduz exceção em
  status. Controllers não capturam nada; o núcleo não sabe que HTTP existe. Toda resposta de
  erro é `ProblemDetail` montado pela `ProblemDetailFactory` (com `timestamp` do `Clock` da
  aplicação); o 401 da cadeia de segurança sai pelo `ProblemDetailAuthenticationEntryPoint`, no mesmo formato.
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
- Constantes compartilhadas ficam em classes que não dependem de ninguém: nomes do OpenAPI em
  `doc/ApiDocumentation` (não na `OpenApiConfig`), caminhos em `route/ApiRoutes` (não nos
  controllers). Assim quem compartilha depende delas, e nenhuma depende de volta — sem ciclo.

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
- Depois de um `forgot-password` num IT, chamar `aguardarProcessamentoEmSegundoPlano()` antes de
  conferir e-mail, token gravado ou a *ausência* deles — o 202 volta antes do processamento.

### Coleção Postman e prints (Etapa 12, já implementada)

- **Endpoint novo entra na coleção** (`postman/Restaurantes.postman_collection.json`): um request
  para o sucesso e um para cada `@ErrorResponse` da operação, na pasta da operação, conferindo
  status e `type`. O JSON da coleção é a fonte de verdade — edite no Postman ou à mão.
- Casos de acesso negado miram um cadastro **descartável criado pela coleção**, nunca a seed; a
  coleção exclui o que cria. Ela precisa poder rodar várias vezes contra o mesmo banco.
- CPF, CNPJ e CNH são únicos: o pré-request **da coleção** gera `{{cpfGerado}}`, `{{cnpjGerado}}` e
  `{{cnhGerada}}` válidos e novos a cada request. Corpo com documento usa essas variáveis, nunca um
  valor fixo (exceto o caso que quer o conflito, que reusa um já gravado).
- Request que produz variável essencial (token, id) confere o status **antes** de ler o corpo e,
  se falhar, faz `postman.setNextRequest(null)` — nada de gravar `undefined` e seguir.
- Efeito de um pedido processado em segundo plano (o e-mail do `forgot-password` no Mailpit) é
  procurado com nova tentativa: o request se repete (`postman.setNextRequest(pm.info.requestName)`,
  com espera no pré-request) até achar ou esgotar as tentativas.
- Validar sempre com `npx newman@6 run ...` (versão fixada) contra a pilha do Compose e, depois,
  gerar os prints da mesma execução:
  `npx newman@6 run postman/Restaurantes.postman_collection.json --reporters cli,json --reporter-json-export target/newman.json`
  e `node postman/gerar-prints.js target/newman.json postman/Restaurantes.postman_collection.json postman/prints`
  (`CHROME_PATH` fora do Windows).
- Rodar a aplicação fora do Docker exige **exportar** o `.env` (Maven e IDE não o leem); o README
  traz os comandos para bash e PowerShell.

## Convenções do domínio (Etapa 2, já implementadas)

- Sem Lombok nem geração de código no núcleo; construtores, fábricas e acessores à mão.
- Entidades: construtor privado + `create(...)` (novo, sem id) e `restore(...)` (com id e
  auditoria), ambos passando pelo mesmo `fill`; setters revalidam. Violação de invariante é
  `InvariantViolationException` (subclasse de `IllegalArgumentException`) via `domain/Guard`
  (`requireNonNull`, `requireNonBlank`, `require`, `trimToNull`) — a mensagem dela chega ao
  cliente, então é escrita para ele. Violação de estado (ex.: token já usado) é
  `IllegalStateException`.
- VOs (`Email`, `ZipCode`, `Cpf`, `Cnpj`, `Phone`, `LicensePlate`, `DriverLicense`) são `record`s com
  construtor compacto que valida e normaliza.
- O domínio recebe o **hash** da senha, nunca a senha; entidades não chamam
  `LocalDateTime.now()` — o instante vem por parâmetro.
- Nenhuma string ou constante de tecnologia no núcleo (hash BCrypt, nome de coluna, JWT).
  Se um caso de uso precisa de um comportamento técnico (ex.: "gaste o tempo de uma
  comparação de senha"), ele vira método da interface de gateway (`IPasswordEncoder.simulateMatch`).
- Atomicidade: o **controller de adaptação** envolve cada `run` em `IUnitOfWork.execute`
  (porta em `application/gateway`); casos de uso não sabem que transação existe. A implementação
  (`TransactionTemplate`) fica em `infrastructure`.
- Exceções de negócio estendem `domain/exception/DomainException`; a tradução para HTTP é
  do handler em `infrastructure/api/rest/spring/exception`.
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
  mocks de `I*Gateway`; adaptadores com mocks de `I*DataSource` e de `adapter/service`.
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
  `infra`, `persistence`, `security`, `api`. Ex.: `feat(domain): adiciona entidades e VOs`,
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
