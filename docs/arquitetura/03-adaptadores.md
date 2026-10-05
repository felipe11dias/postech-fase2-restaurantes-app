# 3. Adaptadores de interface — controllers, gateways, presenters

[← Casos de uso](02-casos-de-uso.md) · Próximo: [Frameworks & Drivers →](04-frameworks-drivers.md)

## 1. O que a aula ensina

- **Aula 02** (pp. 7–9): a camada de adaptadores traduz entre a lógica de negócio e o mundo externo,
  com três componentes:
  - o **controller** recebe a entrada do cliente e aciona o caso de uso certo, sem regra de
    negócio — a aula o compara a um balcão ou a um garçom, que não cozinha mas faz o processo
    acontecer (p. 8);
  - o **gateway** é o tradutor entre caso de uso e sistemas externos (banco, APIs de terceiros,
    outros serviços), implementando a interface que o caso de uso precisa (p. 8). E uma regra de
    implementação explícita: **gateways são sempre instanciados pelos controllers**, porque a origem
    de dados é informada por quem cria o controller (p. 9);
  - o **presenter** prepara o retorno do caso de uso num formato padrão para o cliente, tirando
    essa tarefa do controller (p. 9).
- **Aula 05** (pp. 6–11) mostra os três em código:
  - o controller é criado com uma interface de origem de dados e, **a cada operação**, cria o
    gateway com ela, cria o caso de uso com o gateway, executa e entrega o resultado ao presenter
    (pp. 6–7). É o maestro: não sabe a regra, mas "sabe quem sabe" (p. 7);
  - o gateway implementa a interface do núcleo, recebe a origem de dados por `create` e converte
    o dado externo em entidade — e vice-versa (pp. 7–9);
  - o presenter adapta a entidade para o que o cliente pode ver, escondendo o que não deve sair
    (pp. 10–11).
- **Aula 06** (p. 7): a dependência em abstração vale em todas as camadas — o caso de uso consome
  o gateway por uma interface, e **o gateway consome o serviço externo por outra**. Envio de
  e-mail, pagamento e APIs de terceiros são citados como os serviços externos a isolar.
- **Aula 07** (pp. 6–8): os adaptadores não contêm regra de negócio; o gateway "fala as duas
  línguas" e usa a origem de dados só por interface; o presenter pode cuidar de formatação e
  internacionalização.

## 2. O que os autores dizem

- **Martin, *Clean Architecture*, cap. 22**: a camada de *Interface Adapters* converte dados do
  formato mais conveniente para casos de uso e entidades para o formato de agentes externos, e
  vice-versa; nenhum código daqui para dentro sabe nada do banco.
- **Martin, *Clean Architecture*, cap. 23 (*Presenters and Humble Objects*)**: separar o que é
  difícil de testar (o objeto "humilde" — tela, framework HTTP) do que é fácil (o presenter, que
  monta o modelo de visão). O `@RestController` e o assembler HATEOAS são o objeto humilde; o
  `UserPresenter` é testável sem framework.
- **Freeman e Robson, *Head First Design Patterns*, cap. 7 (Adapter)**: o *Adapter* converte a
  interface de uma classe na interface que o cliente espera. É literalmente o que cada gateway faz:
  o caso de uso espera `IUserGateway` (entidades), a infraestrutura oferece `IUserDataSource`
  (records) — o gateway adapta uma à outra.
- **Freeman e Robson, cap. 1 (Strategy)**: trocar o comportamento por composição, atrás de uma
  interface. Origens de dados e serviços externos são estratégias intercambiáveis que o controller
  recebe prontas.

## 3. Como o projeto implementa

```
adapter/
  controller/   UserController, AuthController              — o maestro
  gateway/      UserGateway, RoleGateway, PasswordResetTokenGateway,
                RestaurantGateway                                     — tradutores de dados
  gateway/mapping/  AddressMapping                                 — tradução compartilhada (endereço)
                PasswordResetMailGateway, TokenGateway                 — tradutores de serviços
  datasource/   IUserDataSource, IRoleDataSource, IPasswordResetTokenDataSource  (+ data/ *Data)
  service/      IMailSender, ITokenEncoder                                        (+ data/ *Data)
  presenter/    UserPresenter, AuthPresenter, RestaurantPresenter, AddressPresenter (+ view/ *View)
```

### Controllers — [`adapter/controller`](../../src/main/java/com/postech/restaurantes/adapter/controller)

Exatamente o formato da Aula 05, com a unidade de trabalho em volta de cada `run`:

```java
public UserView register(NewUserDTO dto) {
    var useCase = RegisterUserUseCase.create(UserGateway.create(userDataSource),
            RoleGateway.create(roleDataSource), passwordEncoder);          // gateways criados aqui
    return UserPresenter.toView(unitOfWork.execute(() -> useCase.run(dto))); // presenter no fim
}
```

O `AuthController` faz o mesmo com os **serviços**: recebe `IMailSender` e `ITokenEncoder` e cria
`PasswordResetMailGateway` e `TokenGateway` a cada operação. Os controllers de adaptação são objetos
comuns, criados pela fábrica estática na composição (`CompositionConfig`) — nunca `@Component`.

### Gateways — [`adapter/gateway`](../../src/main/java/com/postech/restaurantes/adapter/gateway)

| Gateway | Porta do núcleo | Consome | Tradução que faz |
|---|---|---|---|
| `UserGateway` | `IUserGateway` | `IUserDataSource` | `User` ↔ `UserData` (e-mail normalizado, CEP sem máscara, papéis pelo nome, endereços como `UserAddressData` com rótulo e padrão); reconstrói com `User.restore`, que **revalida** |
| `RoleGateway` | `IRoleGateway` | `IRoleDataSource` | `RoleName` ↔ nome textual |
| `RestaurantGateway` | `IRestaurantGateway` | `IRestaurantDataSource` | `Restaurant` ↔ `RestaurantData`, com o endereço do restaurante aninhado |
| `PasswordResetTokenGateway` | `IPasswordResetTokenGateway` | `IPasswordResetTokenDataSource` | `PasswordResetToken` ↔ `PasswordResetTokenData` |
| [`PasswordResetMailGateway`](../../src/main/java/com/postech/restaurantes/adapter/gateway/PasswordResetMailGateway.java) | `IMailGateway` | `IMailSender` | pedido "token + validade" → **assunto e corpo** da mensagem em português |
| [`TokenGateway`](../../src/main/java/com/postech/restaurantes/adapter/gateway/TokenGateway.java) | `ITokenIssuer` | `ITokenEncoder` | `User` → `TokenClaimsData` (id, login, nomes dos papéis) |

**Tradução compartilhada — [`adapter/gateway/mapping`](../../src/main/java/com/postech/restaurantes/adapter/gateway/mapping).**
O endereço é parte de dois agregados (usuário, por `UserAddress`, e restaurante). A tradução
`Address` ↔ `AddressData` é uma só — se o endereço mudar, muda para os dois (duplicação verdadeira,
Martin cap. 16) —, então mora em `AddressMapping`, usado por `UserGateway` e `RestaurantGateway`.
Fica num subpacote porque não é gateway: não implementa porta do núcleo, e a regra
`gateways_do_adapter_implementam_uma_porta_do_nucleo` vale para `adapter.gateway`. Na saída, o
mesmo papel é do `AddressPresenter`.

### Interfaces consumidas pelos gateways

- [`adapter/datasource`](../../src/main/java/com/postech/restaurantes/adapter/datasource): as
  **origens de dados** (o `IDataStorageSource` da Aula 05), em termos de records simples `*Data`.
  Implementadas pelo módulo JPA.
- [`adapter/service`](../../src/main/java/com/postech/restaurantes/adapter/service): os **serviços
  externos** que não são origem de dados — `IMailSender` (só transporte de uma mensagem pronta) e
  `ITokenEncoder` (só codificação de claims prontos). Implementados pelos módulos SMTP e JWT.

### Presenters — [`adapter/presenter`](../../src/main/java/com/postech/restaurantes/adapter/presenter)

`UserPresenter.toView(user)` produz `UserView`, que **não tem campo de senha**: a omissão é
estrutural, não um `if`. `AuthPresenter` acrescenta o esquema `Bearer`. `PageResult.map` preserva
os metadados da página.

### Unidade de trabalho

A atomicidade de um caso de uso com mais de uma escrita é demarcada **aqui**, pelo controller, com
a porta `IUnitOfWork` — porque o caso de uso não deve saber que transação existe, e o adaptador não
pode conhecer o `@Transactional` do Spring. A implementação (`TransactionTemplate`) fica na
infraestrutura.

O que não se desfaz fica **depois** da unidade de trabalho. O e-mail de redefinição passa por uma
[`MailOutbox`](../../src/main/java/com/postech/restaurantes/adapter/controller/MailOutbox.java): o
caso de uso pede o envio normalmente, a caixa guarda a mensagem, e o `AuthController` só a entrega
depois que `unitOfWork.execute` retorna. Se o commit falhar, nenhum e-mail com um token inexistente
sai; e a transação não fica aberta esperando o servidor de e-mail.

## 4. Padrões adotados e por quê

| Padrão | Onde | Problema que resolve |
|---|---|---|
| **Adapter** (Freeman cap. 7) | todos os gateways | o caso de uso fala em entidades; a infraestrutura, em records e texto — o gateway traduz |
| **Strategy** por interface (Freeman cap. 1) | `I*DataSource`, `IMailSender`, `ITokenEncoder` recebidos pelo controller | trocar JPA, SMTP ou JWT não muda o adaptador (Aula 06) |
| **Humble Object** (Martin cap. 23) | presenter testável × `@RestController`/assembler "humildes" | a lógica de apresentação é testada sem framework |
| Fábrica estática `create` | controllers e gateways | mesma forma das aulas; dependências validadas na criação |
| Controller instancia o gateway por operação | `UserController`, `AuthController` | regra explícita da Aula 02 (p. 9); o gateway é barato e sem estado |

## 5. Desvios conscientes

**Portas técnicas implementadas direto pela infraestrutura.** `IPasswordEncoder`,
`ISecureTokenGenerator` e `IUnitOfWork` não passam por um gateway no adaptador. Pelo padrão das
aulas, cada uma teria um gateway que só repassaria a chamada (texto → texto): não haveria tradução
nenhuma, e o gateway seria indireção sem propósito (*Clean Code*: cada camada de abstração deve
fazer uma coisa). O critério é **haver tradução**: e-mail (o que o usuário lê) e token (quem é o
portador) têm; hash de senha, geração de número aleatório e transação não têm. A regra do build
(abaixo) congela exatamente essa lista — uma porta nova com tradução **precisa** ganhar gateway.

Até a Etapa 13, e-mail e token também eram implementados direto pela infraestrutura, e o texto do
e-mail morava no módulo SMTP. A revisão de conformidade com as aulas (Etapa 14 do relatório) moveu os
dois para o adaptador.

Outros pontos em relação ao código das aulas:

| Na aula | No projeto | Por quê |
|---|---|---|
| Presenter devolve `EstudanteDTO` (Aula 05, p. 10) | presenter devolve `*View` | separa o que sai do núcleo dos DTOs que entram |
| Presenter ofusca a identificação (Aula 05, p. 10) | presenter omite a senha por construção | o mesmo princípio — o presenter decide o que o cliente vê — aplicado ao dado sensível do projeto |
| Gateway lança "não encontrado" (Aula 05, p. 8) | gateway devolve `Optional`; quem decide que é erro é o caso de uso | "não encontrado" é regra de aplicação; o gateway só traduz |

## 6. Como o build verifica

- `ArchitectureTest`: `adapter` não depende de `infrastructure`; toda classe em `adapter.gateway`
  implementa uma interface de `application.gateway`; `adapter.datasource` e `adapter.service` só
  têm interfaces com prefixo `I`; seus subpacotes `data` só têm records com sufixo `Data`; views
  são records com sufixo `View`.
- `InfrastructureModulesTest`: **a infraestrutura só conhece as portas técnicas** do núcleo — qualquer
  outra porta implementada (ou referenciada, como uma lambda num `@Bean`) fora de `adapter.gateway`
  quebra o build; e os módulos de e-mail e token não conhecem o `domain`.
- Testes: `UserGatewayTest`, `RoleAndTokenGatewaysTest`, `ServiceGatewaysTest` (tradução com os
  serviços mockados), `PresentersTest`, e `UserControllerTest`/`AuthControllerTest` (inclusive: e-mail só depois do commit, e nenhum se
  o commit falha), que testam o
  controller "de ponta a ponta dentro do núcleo" — origens de dados e serviços mockados, casos de
  uso, gateways e presenters reais.
