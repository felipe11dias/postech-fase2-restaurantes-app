# 4. Frameworks & Drivers — os detalhes

[← Adaptadores de interface](03-adaptadores.md) · Próximo: [Princípios →](05-principios.md)

## 1. O que a aula ensina

- **Aula 02** (pp. 9–11): a camada mais externa implementa os detalhes técnicos — banco de dados,
  sistema de arquivos, rede, API REST, integração com APIs externas. Ela pode ser tratada como
  "detalhe" porque a dependência é invertida: as camadas internas não a conhecem, e trocar o banco
  acontece só aqui (pp. 9–10). As origens de dados implementam, com a tecnologia concreta, as
  interfaces que as camadas de dentro definiram (p. 10). Frameworks como o Spring ajudam a injetar
  dependências, mas devem ficar **restritos a esta camada** (p. 11).
- **Aula 06** (pp. 6–8):
  - independência de framework: nem a regra de negócio **nem a camada de adaptação** podem depender
    de funcionalidade de framework (pp. 6–7);
  - integrações com serviços externos (APIs, pagamento, e-mail) são detalhes na borda, isolados de
    modo que seja fácil integrá-los e desacoplá-los — o isolamento traz resiliência de brinde (p. 7);
  - o banco de dados é um detalhe como qualquer outro; a facilidade de ligar o framework de
    persistência às entidades cobra o preço do acoplamento (p. 8).

## 2. O que os autores dizem

- **Martin, *Clean Architecture***
  - cap. 17 (*Boundaries*): cada detalhe é um *plugin* das regras de negócio; a fronteira é
    desenhada onde as coisas mudam por motivos diferentes;
  - cap. 26 (*The Main Component*): o componente Main é o mais "sujo" do sistema — cria tudo,
    injeta tudo e é o único que conhece todos os outros;
  - cap. 30, 31 e 32: **o banco, a web e os frameworks são detalhes**. "Case-se" com o framework
    só na borda, e mantenha-o atrás de fronteiras.
- **Date, *Introdução a Sistemas de Bancos de Dados***: formas normais e integridade referencial
  guiam o schema.
- **Machado, *Banco de Dados: Projeto e Implementação***: projeto em três níveis (conceitual,
  lógico, físico); o modelo de dados do sistema não é o modelo de objetos do domínio.

## 3. Como o projeto implementa

A infraestrutura é um conjunto de **módulos-plugin**: o pacote diz o papel, o subpacote a
tecnologia. Trocar uma tecnologia é apagar um subpacote e criar outro ao lado.

| Módulo | Implementa | Tecnologia | Para trocar |
|---|---|---|---|
| [`main`](../../src/main/java/com/postech/restaurantes/infrastructure/main) | a composição (Main, cap. 26): `CompositionConfig`, `PasswordResetProperties` | Spring (`@Configuration`) | — é o único que conhece todos |
| [`web`](../../src/main/java/com/postech/restaurantes/infrastructure/web) | entrega HTTP: `api/<feature>` (`@RestController`, `*Request`/`*Response`, assembler HATEOAS), `error` (ProblemDetail), `doc` (OpenAPI), `validation`, `security` | Spring MVC, Spring Security, springdoc, Bean Validation | outro canal (gRPC, CLI) chama os mesmos controllers de adaptação |
| [`persistence/jpa`](../../src/main/java/com/postech/restaurantes/infrastructure/persistence/jpa) | `I*DataSource` (`UserDataSourceJpa`, …) e `IUnitOfWork` (`TransactionalUnitOfWork`); `audit` (colunas de auditoria) | JPA/Hibernate, Spring Data, PostgreSQL | `persistence/jdbc` com as mesmas interfaces; o schema continua do Flyway |
| [`token/jwt`](../../src/main/java/com/postech/restaurantes/infrastructure/token/jwt) | `ITokenEncoder` e `IAccessTokenReader` (`JwtTokenEncoder`) | jjwt | `token/<outro>` implementando as duas interfaces |
| [`crypto`](../../src/main/java/com/postech/restaurantes/infrastructure/crypto) | `IPasswordEncoder` (`BCryptPasswordAdapter`), `ISecureTokenGenerator` (`SecureRandomTokenGenerator`) | spring-security-crypto, JDK | nova classe (ex.: Argon2) implementando a porta |
| [`mail/smtp`](../../src/main/java/com/postech/restaurantes/infrastructure/mail/smtp) | `IMailSender` (`SmtpMailSender`) — **só transporte** | Spring Mail | `mail/<provedor>` implementando `IMailSender`; o texto do e-mail está no adaptador e não muda |

**Duas fronteiras de mapeamento.** As entidades JPA (`UserJpaEntity`, …) são classes separadas
das entidades de domínio, só com mapeamento; os DTOs HTTP (`NewUserRequest`, `UserResponse`) são
separados dos DTOs dos casos de uso. Nenhuma anotação atravessa para dentro — o preço de
"facilidade" que a Aula 06 (p. 8) alerta não é pago.

**Quando um módulo precisa de outro**, ele declara a interface e o `main` liga as pontas: o autor
da auditoria (persistência) vem de `web/security/AuthenticatedActor` por um `Supplier` ligado na
`CompositionConfig`; o filtro HTTP lê o token pela porta `IAccessTokenReader`, que ele mesmo
declara e o módulo JWT implementa.

**Schema versionado.** O Flyway é o dono do schema
([`V1__create_schema.sql`](../../src/main/resources/db/migration/V1__create_schema.sql)); o JPA só
valida (`ddl-auto: validate`). O V1 documenta a normalização: papéis e endereços em tabelas
próprias (1FN), a chave composta `user_roles` sem dependência parcial (2FN), nada derivável em
`users` (3FN) e todo determinante — e-mail, login — como chave candidata (BCNF). A seed
(`V2__seed_demo_users.sql`) cria os usuários de demonstração.

**Execução.** Docker Compose sobe a aplicação, o PostgreSQL e o Mailpit (SMTP de testes), com
imagens de versão fixa e portas só em `127.0.0.1`.

## 4. Padrões adotados e por quê

| Padrão | Onde | Problema que resolve |
|---|---|---|
| Plugin por módulo (Martin cap. 17) | um subpacote por tecnologia | trocar uma tecnologia não toca em outra |
| Main / raiz de composição (cap. 26) | `infrastructure/main` | só um lugar conhece tudo; os demais são substituíveis |
| Porta declarada pelo cliente dentro da borda (DIP) | `IAccessTokenReader` em `web/security` | a web não conhece JWT |
| Data mapper em duas camadas | `*JpaEntity` + gateway | domínio sem anotação; o mapeamento de banco muda sem mexer na entidade |
| Busca paginada em duas consultas | `UserDataSourceJpa.search` | `join fetch` com paginação faria o Hibernate recortar a página em memória |

## 5. Desvios conscientes

- A aula (p. 11 da Aula 02) cita o Spring como o que facilita a injeção de dependências. O projeto
  usa o Spring só para **montar** a infraestrutura; os controllers de adaptação são criados pelas
  fábricas estáticas dentro da `CompositionConfig`, e não por *component scan* — o núcleo inteiro
  poderia ser montado à mão, sem Spring.
- O banco tem `DEFAULT gen_random_uuid()` nas chaves, mas o id de uma inserção pela aplicação é
  gerado pelo Hibernate (`@GeneratedValue(strategy = UUID)`); o default do banco serve à inserção
  direta por SQL, como a seed.
- Erros do próprio Spring MVC (405, 415, rota inexistente) saem com `type: about:blank` e título em
  inglês — válido pela RFC 9457, mas é o único ponto em que a API não responde em português.

## 6. Como o build verifica

[`InfrastructureModulesTest`](../../src/test/java/com/postech/restaurantes/InfrastructureModulesTest.java)
(15 regras): nenhum ciclo entre pacotes; `persistence`, `crypto` e `mail` não conhecem outro
módulo; `token` só conhece, de `web`, a porta que implementa; `web` não conhece implementações;
cada biblioteca só no seu módulo (JPA em `persistence`, jjwt em `token.jwt`, Spring Mail em
`mail.smtp`, Spring Security em `web`/`crypto`, Spring MVC/springdoc em `web`); `*Config` ⇔
`@Configuration`; e a infraestrutura só implementa diretamente as portas técnicas.
Testes de integração (Testcontainers, PostgreSQL real) provam os módulos juntos — ver
[Testes](06-testes.md).
