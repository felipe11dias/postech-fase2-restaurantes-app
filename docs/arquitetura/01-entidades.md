# 1. Entidades — *Enterprise Business Rules*

[← Visão geral](00-visao-geral.md) · Próximo: [Casos de uso →](02-casos-de-uso.md)

## 1. O que a aula ensina

- **Aula 02** (pp. 6–7): as Entidades são o coração do sistema — objetos e regras de negócio
  essenciais, modelos puros sem tecnologia externa, estáveis, que só mudam quando a regra muda. A
  entidade é **responsável por validar os próprios dados**: se a regra diz que o estudante tem de
  ser maior de idade ou ter e-mail válido, não pode existir no sistema uma instância que a viole.
- **Aula 03** (pp. 5–11) aprofunda:
  - entidade **não é mapeamento de banco**, e sim a implementação das regras críticas comuns à
    aplicação, válidas em qualquer forma de apresentação ou armazenamento (p. 5);
  - o exemplo `Estudante` tem **duas fábricas estáticas**: uma para o objeto novo (sem
    identificação) e outra que recebe a identificação (para o objeto que já existe); ambas validam,
    e **cada setter revalida** antes de atribuir (pp. 5–8);
  - a independência de componentes externos facilita manutenção, deixa a entidade próxima da
    **linguagem do negócio** — a aula liga isso à linguagem ubíqua do DDD (pp. 8–9);
  - entidades são testáveis com testes unitários simples, sem mocks, e permitem TDD (pp. 9–11).

## 2. O que os autores dizem

- **Martin, *Clean Architecture*, cap. 20 (*Business Rules*)**: uma entidade encapsula as
  *Critical Business Rules* e os *Critical Business Data* sobre os quais elas operam; é o código
  mais independente e reutilizável do sistema, e não sabe nada sobre banco, UI ou frameworks.
- **Martin, *Clean Code*, cap. 6 (*Objects and Data Structures*)**: objetos escondem dados e expõem
  comportamento; estruturas de dados expõem dados e não têm comportamento. As entidades do domínio
  são objetos; os records de transporte (`*Data`, `*DTO`, `*View`) são estruturas de dados — e o
  projeto mantém os dois papéis separados.
- **Martin, *Clean Code*, cap. 7 (*Error Handling*)**: use exceções, não códigos de retorno; o
  domínio lança exceções com significado de negócio.

## 3. Como o projeto implementa

| Elemento | Onde | Regra que carrega |
|---|---|---|
| [`User`](../../src/main/java/com/postech/restaurantes/domain/entity/user/User.java) | `domain/entity/user` | nome, e-mail e login válidos; **os perfis** (`UserProfiles`); o papel é **derivado** dos perfis (`getRoles()`), nunca guardado; endereços válidos e, havendo endereços, **exatamente um padrão**; endereço com id só se já for do usuário; recebe o *hash* da senha, nunca a senha |
| [`UserAddress`](../../src/main/java/com/postech/restaurantes/domain/entity/user/UserAddress.java) | `domain/entity/user` | parte do agregado de usuário: rótulo opcional, marca de padrão e o `Address`; quantos são padrão é regra do `User`, não de um endereço isolado |
| [`UserProfiles`](../../src/main/java/com/postech/restaurantes/domain/entity/user/UserProfiles.java) | `domain/entity/user` | os perfis do usuário (Etapa 21): cada um opcional, **ao menos um** (especialização total) e vários ao mesmo tempo (sobreposta); cliente e entregador com **o mesmo CPF**; calcula os papéis, sempre na mesma ordem. Incluir, trocar e tirar perfil (`withOwner`… `withAdmin`, `without`, Etapa 22) devolvem um conjunto novo, validado pelas mesmas regras — tirar o último é recusado aqui |
| [`ProfileType`](../../src/main/java/com/postech/restaurantes/domain/entity/user/ProfileType.java) | `domain/entity/user` | os quatro tipos de perfil, com o nome usado nas mensagens; `from(String)` recusa tipo desconhecido |
| `UserProfiles.withClient` / `withCourier` (Etapa 22) | `domain/entity/user` | o CPF é da pessoa: *alterar* o CPF de um perfil corrige o do outro, em cópia (o original não muda); *incluir* com CPF diferente continua recusado |
| [`RoleName`](../../src/main/java/com/postech/restaurantes/domain/entity/role/RoleName.java) | `domain/entity/role` | os quatro papéis de autorização (`ROLE_OWNER`, `ROLE_CLIENT`, `ROLE_COURIER`, `ROLE_ADMIN`), um por perfil; não há mais catálogo de papéis nem entidade `Role` |
| [`PasswordResetToken`](../../src/main/java/com/postech/restaurantes/domain/entity/password/PasswordResetToken.java) | `domain/entity/password` | vence no instante informado; usável só uma vez (`markUsed`); **um por usuário**: o pedido novo o reemite (`reissue`: hash e validade novos, uso zerado, tudo validado antes de mudar); o **instante vem por parâmetro** — a entidade não consulta o relógio |
| [`Address`](../../src/main/java/com/postech/restaurantes/domain/entity/address/Address.java) | `domain/entity/address` | campos obrigatórios, UF com 2 letras, CEP válido; pacote próprio porque é compartilhado: o usuário o tem por `UserAddress`, o restaurante, diretamente — e não conhece nenhum dos dois |
| [`OfficeHour`](../../src/main/java/com/postech/restaurantes/domain/entity/restaurant/OfficeHour.java) | `domain/entity/restaurant` | horário de funcionamento (Etapa 24), valor: dia (`java.time.DayOfWeek`), abertura e fechamento diferentes; fechamento antes da abertura vira a meia-noite. A sobreposição é regra do conjunto, na raiz: `Restaurant.replaceOfficeHours` exige ao menos um horário e nenhum par sobreposto na semana circular |
| [`OwnerProfile`](../../src/main/java/com/postech/restaurantes/domain/entity/owner/OwnerProfile.java), [`ClientProfile`](../../src/main/java/com/postech/restaurantes/domain/entity/client/ClientProfile.java), [`CourierProfile`](../../src/main/java/com/postech/restaurantes/domain/entity/courier/CourierProfile.java), [`AdminProfile`](../../src/main/java/com/postech/restaurantes/domain/entity/admin/AdminProfile.java) | `domain/entity/{owner,client,courier,admin}` | perfis do usuário (especializações do modelo v2), sem id próprio — a identidade é a do usuário. Cliente: nascimento não futuro, com o dia **por parâmetro**; entregador: CNH e placa andam com o veículo (`changeVehicle`) e começa `OFFLINE`; admin: código de funcionário obrigatório |
| [`CourierVehicleType`](../../src/main/java/com/postech/restaurantes/domain/entity/courier/CourierVehicleType.java), [`CourierStatus`](../../src/main/java/com/postech/restaurantes/domain/entity/courier/CourierStatus.java) | `domain/entity/courier` | os enums do modelo de dados (`courier_vehicle_type`, `courier_status`); `requiresLicense()` diz quais veículos exigem CNH e placa; `from(String)` recusa valor desconhecido com mensagem do domínio |
| [`Email`](../../src/main/java/com/postech/restaurantes/domain/vo/Email.java), [`ZipCode`](../../src/main/java/com/postech/restaurantes/domain/vo/ZipCode.java), `Cpf`, `Cnpj`, `Phone`, `LicensePlate`, `DriverLicense` | `domain/vo` | *records* que validam e **normalizam** no construtor (e-mail em minúsculas; CEP, CPF, telefone e CNH só com dígitos; CPF e CNPJ com verificadores conferidos — CNPJ também no formato alfanumérico de 2026; placa antiga ou Mercosul) |
| [`Guard`](../../src/main/java/com/postech/restaurantes/domain/Guard.java) | `domain` | `requireNonNull`, `requireNonBlank`, `require`, `trimToNull` — lançam `InvariantViolationException` |
| [`domain/exception`](../../src/main/java/com/postech/restaurantes/domain/exception) | `domain/exception` | `DomainException` e subclasses (`DuplicateResourceException`, `ResourceNotFoundException`, …); `ResourceInUseException` (recurso de que outro depende, Etapa 22); `InvariantViolationException` para invariante violada |

Exemplo — as duas fábricas de `User`, o equivalente direto do `Estudante` da Aula 03:

```java
public static User create(String name, String email, String login, String passwordHash,
                          UserProfiles profiles, List<UserAddress> addresses) { // novo, sem id
    return fill(new User(null, null, null), name, email, login, passwordHash, profiles, addresses);
}

public static User restore(UUID id, String name, String email, String login, String passwordHash,
                           UserProfiles profiles, List<UserAddress> addresses,
                           LocalDateTime createdAt, LocalDateTime lastUpdatedAt) { // já existe
    Guard.requireNonNull(id, "Id do usuário inválido");
    ...
    return fill(user, name, email, login, passwordHash, profiles, addresses); // mesma validação
}
```

**Regra de negócio × regra de aplicação.** O que vale em qualquer contexto (e-mail válido, ao menos
um perfil, CPF igual entre cliente e entregador, CEP de 8 dígitos) mora na entidade. O que depende do
ponto de entrada (perfil de administrador fora do *autocadastro*, CPF e CNPJ únicos, resposta idêntica no "esqueci minha senha") mora no caso de uso — ver
[Casos de uso](02-casos-de-uso.md).

## 4. Padrões adotados e por quê

| Padrão | Onde | Problema que resolve |
|---|---|---|
| Fábricas estáticas `create` / `restore` com construtor privado | `User`, os perfis, `PasswordResetToken`, `Address` | toda instância passa por validação; não existe `new User()` inválido (Aula 03) |
| *Value Object* como `record` | `Email`, `ZipCode`, `Cpf`, `Cnpj`, `Phone`, `LicensePlate`, `DriverLicense`; `UserProfiles` | o valor é validado e normalizado uma vez, e dois e-mails iguais são iguais |
| Cláusula de guarda | `Guard` | a validação lê como uma frase e sempre lança a mesma exceção, cuja mensagem é escrita para o usuário final |
| Setter que revalida | `User.setName`, `setEmail`, … | a entidade continua consistente depois de criada (Aula 03, p. 8) |
| Tempo por parâmetro | `PasswordResetToken.create(..., now)`, `isUsable(now)` | a regra de vencimento é testável sem relógio e sem mocks |

## 5. Desvios conscientes

| Na aula | No projeto | Por quê |
|---|---|---|
| `@Getter`, `@EqualsAndHashCode` do Lombok no `Estudante` (Aula 03, p. 5) | acessores escritos à mão; nenhuma biblioteca no `domain` | a própria Aula 06 pede que as camadas internas não dependam de framework; o ArchUnit proíbe qualquer import de biblioteca no domínio |
| Duas sobrecargas de `create`, uma com a identificação (Aula 03, p. 6) | `create` (novo) e `restore` (existente) | o nome diz a intenção (*Clean Code* cap. 2); e o `restore` **também** valida — no exemplo da aula a versão com identificação não chama as validações, e um registro corrompido no banco viraria entidade inválida em memória |
| Validação de e-mail por biblioteca (`EmailValidator`, Aula 03, p. 7) | `Email` é um VO próprio | o domínio não importa biblioteca |
| `IllegalArgumentException` direto | `InvariantViolationException` (subclasse) via `Guard` | a infraestrutura distingue a mensagem escrita para o cliente de uma mensagem interna de biblioteca, e só a primeira chega à resposta HTTP |

## 6. Como o build verifica

- `ArchitectureTest`: `domain` não depende de nenhum outro pacote do projeto nem de biblioteca
  (só o JDK); tudo em `domain.vo` é `record` (`objetos_de_valor_sao_records`, Etapa 25).
- Testes unitários sem mocks, um por invariante (aceita o válido, recusa o inválido):
  [`UserTest`](../../src/test/java/com/postech/restaurantes/domain/entity/user/UserTest.java),
  `UserAddressTest`, `UserProfilesTest`, `PasswordResetTokenTest`, `AddressTest`,
  `RestaurantTest`, `OwnerProfileTest`, `ClientProfileTest`, `CourierProfileTest`, `AdminProfileTest`,
  `CourierEnumsTest`, `CpfTest`, `CnpjTest`, `PhoneLicensePlateDriverLicenseTest`, `EmailTest`,
  `ZipCodeTest`, `GuardTest`, `DomainExceptionsTest`.
