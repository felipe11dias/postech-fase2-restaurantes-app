# 2. Casos de uso — *Application Business Rules*

[← Entidades](01-entidades.md) · Próximo: [Adaptadores de interface →](03-adaptadores.md)

## 1. O que a aula ensina

- **Aula 02** (p. 7): a camada de casos de uso responde "que ações o usuário pode realizar e como
  o sistema deve responder"; contém processos, regras e exceções que determinam **como** as
  entidades interagem. O exemplo "processar pagamento de matrícula" mostra o caso de uso
  orquestrando entidades e um serviço externo. A p. 10 acrescenta: as camadas internas **definem
  as interfaces** (contratos) de que precisam para salvar ou recuperar dados, sem saber como
  isso é implementado.
- **Aula 03** (pp. 11–14):
  - o caso de uso encapsula a lógica **específica** da aplicação — uma funcionalidade com valor
    para o usuário (p. 11);
  - o exemplo `CadastrarEstudanteUseCase` tem construtor privado, fábrica estática
    `create(IEstudanteGateway)` e o método `run(NovoEstudanteDTO)`, que consulta o gateway, lança
    uma exceção de negócio se já existir, cria a entidade e pede ao gateway para incluí-la
    (pp. 11–12);
  - ele recebe a solicitação do controller, usa as entidades, requisita dados pelos gateways e
    devolve a resposta; sequencia passos e garante as condições antes de concluir (p. 12);
  - testes unitários com o gateway **mockado** e as três fases *arrange/act/assert* (pp. 13–14);
    a aula menciona que a camada permite até BDD (p. 14).
- **Aula 07** (p. 5): casos de uso coordenam as entidades e se comunicam com as fontes de dados,
  isolados de qualquer infraestrutura ou interface.

## 2. O que os autores dizem

- **Cockburn, *Writing Effective Use Cases***: um caso de uso é o **contrato de comportamento** do
  sistema diante de um **ator** que tem um **objetivo**; é escrito no nível de objetivo do usuário
  (uma tarefa completa, "nível do mar"), com um **cenário principal de sucesso** e as
  **extensões** — tudo o que pode dar diferente em cada passo e como o sistema responde.
- **Martin, *Clean Architecture*, cap. 20**: os casos de uso contêm as regras de negócio
  **específicas da aplicação**; orquestram as entidades, não sabem como os dados chegam nem como
  saem, e recebem e devolvem estruturas de dados simples.
- **Martin, *Clean Architecture*, cap. 11 (DIP)** e ***Agile PPP***: quem usa a abstração a
  declara — por isso as interfaces de gateway ficam em `application`, ao lado dos casos de uso que
  as consomem.

## 3. Como o projeto implementa

Um caso de uso por **intenção do ator**, em subpacotes por feature:

| Ator — objetivo | Caso de uso | Cenário principal | Extensões (exceções de negócio) |
|---|---|---|---|
| Visitante — cadastrar-se | [`RegisterUserUseCase`](../../src/main/java/com/postech/restaurantes/application/usecase/user/RegisterUserUseCase.java) | perfis pedidos (dono, cliente, entregador; nunca administrador) → e-mail, login, CPF e CNPJ livres → `User.create` com hash da senha → `insert` | e-mail, login, CPF ou CNPJ repetido (`DuplicateResourceException`); nenhum perfil, documento inválido, CPFs divergentes ou veículo sem CNH e placa (invariantes do domínio) |
| Usuário — consultar/atualizar/excluir o próprio cadastro | `FindUserByIdUseCase`, `UpdateUserUseCase`, `DeleteUserUseCase` | busca, altera dados e endereços, remove (com os restaurantes dele, pela porta do restaurante) | inexistente; e-mail/login de outro cadastro; excluir o último administrador (`ResourceInUseException`) |
| Usuário ou administrador — manter os perfis do cadastro | `SaveUserProfileUseCase`, `RemoveUserProfileUseCase` | inclui ou altera um perfil (entrada `UserProfileDTO`, interface selada, um `case` por tipo); remove um perfil | CPF/CNPJ de outro cadastro (`DuplicateResourceException`); perfil que o usuário não tem (`ResourceNotFoundException`); último perfil (invariante); dono com restaurante (`ResourceInUseException`, pela porta `IRestaurantGateway`) |
| Entregador — informar a disponibilidade | `ChangeCourierStatusUseCase` | troca o status (`CourierStatus.from`) | quem não é entregador; status desconhecido |
| Visitante, dono ou administrador — consultar restaurantes | `FindRestaurantByIdUseCase`, `SearchRestaurantsUseCase` | por id, ou paginado com busca por nome e, opcionalmente, só os de um dono | restaurante inexistente |
| Regra de posse — saber de quem é o restaurante | `FindRestaurantOwnerUseCase` | o dono do restaurante, para a borda decidir se o requisitante pode alterar ou excluir | inexistente não tem dono (vazio, não exceção) |
| Usuário — trocar a senha | `ChangePasswordUseCase` | confere a atual → confirmação → novo hash | senha atual errada, confirmação divergente (`InvalidPasswordException`) |
| Administrador — listar cadastros | `SearchUsersUseCase` | busca paginada por nome | ordenação por propriedade não permitida cai no nome |
| Usuário — entrar | [`AuthenticateUseCase`](../../src/main/java/com/postech/restaurantes/application/usecase/auth/AuthenticateUseCase.java) | login existe, senha confere → token | credencial inválida — **mesma mensagem e mesmo tempo** para login inexistente e senha errada |
| Usuário — esqueceu a senha | [`ForgotPasswordUseCase`](../../src/main/java/com/postech/restaurantes/application/usecase/auth/ForgotPasswordUseCase.java) | gera token, grava só o hash (reemite o token que o usuário já tinha — o anterior deixa de valer), envia o valor em claro por e-mail | e-mail desconhecido: **mesma resposta**, nada é enviado |
| Usuário — redefinir a senha | [`ResetPasswordUseCase`](../../src/main/java/com/postech/restaurantes/application/usecase/auth/ResetPasswordUseCase.java) | token válido → invalida o token → grava o novo hash | token desconhecido, vencido ou usado; confirmação divergente |

**As portas** que os casos de uso declaram, em
[`application/gateway`](../../src/main/java/com/postech/restaurantes/application/gateway):

| Porta | O que o caso de uso pede | Implementada por |
|---|---|---|
| `IUserGateway` (inclusive `findByCpf`, `findByCnpj`), `IPasswordResetTokenGateway`, `IRestaurantGateway` | encontrar, incluir, alterar, excluir agregados | gateways do adaptador sobre origens de dados (`UserGateway`, …) |
| `IMailGateway` | "mande este token a este e-mail, válido por tanto tempo" | `PasswordResetMailGateway` (adaptador) sobre `IMailSender` |
| `ITokenIssuer` | "emita o token de acesso deste usuário" | `TokenGateway` (adaptador) sobre `ITokenEncoder` |
| `IPasswordEncoder` | gerar e conferir hash; gastar o tempo de uma comparação | direto pela infraestrutura (`crypto`) — porta técnica |
| `ISecureTokenGenerator` | gerar token aleatório e o seu hash | direto pela infraestrutura (`crypto`) — porta técnica |
| `IUnitOfWork` | "estas escritas acontecem juntas ou nenhuma" | direto pela infraestrutura (`persistence/jpa`) — porta técnica |

**Os dados de entrada** são records próprios em
[`application/dto`](../../src/main/java/com/postech/restaurantes/application/dto)
(`NewUserDTO`, `CredentialsDTO`, …), e a paginação também é do núcleo (`PageRequest`,
`PageResult`) — `Pageable`/`Page` do Spring só existem na infraestrutura.

Exemplo — o equivalente do `CadastrarEstudanteUseCase` da Aula 03:

```java
public final class RegisterUserUseCase {
    private RegisterUserUseCase(IUserGateway userGateway, IPasswordEncoder passwordEncoder,
                                Clock clock) { ... }

    public static RegisterUserUseCase create(IUserGateway userGateway, IPasswordEncoder passwordEncoder,
                                             Clock clock) { ... }

    public User run(NewUserDTO dto) {
        // regra de aplicação: o pedido não tem perfil de administrador (autocadastro não o concede)
        // extensões: e-mail, login, CPF e CNPJ já cadastrados
        // cenário principal: User.create(... passwordEncoder.encode(senha) ...) → userGateway.insert
    }
}
```

## 4. Padrões adotados e por quê

| Padrão | Onde | Problema que resolve |
|---|---|---|
| Um caso de uso por classe, `create` + `run` | todos os `*UseCase` | uma intenção do ator por classe (SRP); dependências explícitas na fábrica (Aula 03) |
| Porta declarada pelo consumidor | `application/gateway` | o núcleo diz o que precisa, a infraestrutura se adapta (DIP) |
| Extensões como exceções de domínio | `DomainException` e subclasses | cada desvio do cenário principal tem nome de negócio e vira uma categoria de erro HTTP no handler |
| Regra de negócio × regra de aplicação | entidade × caso de uso | o que vale sempre fica na entidade; o que depende do ponto de entrada fica no caso de uso |
| Relógio e validade por parâmetro | `ForgotPasswordUseCase.create(..., validity, clock)`, `RegisterUserUseCase.create(..., clock)` | vencimento e "nascimento não futuro" testáveis com `Clock.fixed`, sem esperar o tempo passar |
| Regra de aplicação compartilhada (Etapas 22 e 25) | `application/policy/user/UniqueDocumentsPolicy` (`RegisterUserUseCase`, `SaveUserProfileUseCase`) e `LastAdminPolicy` (`RemoveUserProfileUseCase`, `DeleteUserUseCase`) | "CPF e CNPJ únicos" e "o sistema não fica sem administrador" existem uma vez só (DRY); não é caso de uso — não é objetivo do ator nem tem `run` —, então mora num pacote de políticas da camada, que depende só do domínio e das portas |
| Papéis atuais a cada requisição (Etapa 22) | `FindCurrentRolesUseCase` | a autorização usa os perfis gravados agora, não os do token: perfil removido deixa de valer na hora |

## 5. Desvios conscientes

| Na aula | No projeto | Por quê |
|---|---|---|
| O controller da aula captura a exceção de negócio e devolve `null` (Aula 05, p. 7) | a exceção sobe até o handler global da infraestrutura | `null` apaga o motivo; a exceção de domínio vira um `ProblemDetail` com categoria própria, e o núcleo continua sem saber que HTTP existe |
| O gateway da aula devolve `null` quando não encontra (Aula 03, p. 12; Aula 05, p. 8) | `Optional<User>` | a ausência fica explícita no tipo, e o caso de uso decide se é erro |
| Transação não aparece nas aulas | `IUnitOfWork`, usada pelo **controller** em volta de cada `run` | casos de uso com mais de uma escrita (redefinir senha) precisam ser atômicos; a demarcação é regra de aplicação, a transação é detalhe (ver [Adaptadores](03-adaptadores.md)) |
| O caso de uso da aula recebe só o gateway de dados | casos de uso também recebem portas técnicas (`IPasswordEncoder`, `ISecureTokenGenerator`, `Clock`) | o que o caso de uso precisa do mundo técnico entra por interface declarada no núcleo; nenhuma constante de tecnologia (BCrypt, SHA-256) aparece aqui |

## 6. Como o build verifica

- `ArchitectureTest`: `application` depende apenas de `domain`; classes em `..usecase..` terminam
  em `UseCase` e têm **`run` como único método público de instância**; tudo em
  `application.gateway` é interface com prefixo `I`; em `application.policy`, só classes `*Policy`, usadas só
  por casos de uso e outras políticas (`politicas_de_aplicacao_terminam_em_Policy`, Etapa 25).
- Testes unitários com as portas mockadas e AAA, um comportamento por teste:
  `RegisterUserUseCaseTest`, `UpdateUserUseCaseTest`, `ChangePasswordUseCaseTest`,
  `UserQueryUseCasesTest`, `SaveUserProfileUseCaseTest`, `RemoveUserProfileUseCaseTest`, `UniqueDocumentsPolicyTest`,
  `LastAdminPolicyTest`, os testes de restaurante, `AuthenticateUseCaseTest`, `ForgotPasswordUseCaseTest`,
  `ResetPasswordUseCaseTest`, e `PageRequestTest`/`PageResultTest`/`DtoTest` para os DTOs.
