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
- 77 testes unitários com Mockito e `Clock.fixed`; cobertura acumulada 100%.
- Correções da revisão de código: `ResetPasswordUseCase` invalida o token antes de gravar a
  senha; elementos nulos em `roles`/`addresses` viram 400 em vez de NPE; `AuthenticateUseCase`
  compara contra um hash fictício quando o login não existe (mesmo tempo de resposta) e trata
  senha em branco como credencial inválida; `ChangePasswordUseCase` trata senha atual em branco
  como incorreta.
- Revisão de arquitetura: `IPasswordEncoder.simulateMatch` substitui o hash BCrypt constante que
  havia entrado no caso de uso (o núcleo volta a não conhecer o algoritmo); pacotes de
  `domain`/`application` reorganizados em subpacotes por agregado/feature (`user`, `auth`,
  `address`, `common`) — *screaming architecture*.
