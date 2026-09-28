# 5. Princípios — o que sustenta as camadas

[← Frameworks & Drivers](04-frameworks-drivers.md) · Próximo: [Testes →](06-testes.md)

## 1. O que a aula ensina

A **Aula 06** reúne os princípios de design sobre os quais a Clean Architecture se apoia:

| Princípio (Aula 06) | Ideia | Página |
|---|---|---|
| Organização em camadas | camadas concêntricas com regras claras de dependência | p. 5 |
| Inversão de dependências | camadas externas dependem de abstrações definidas nas internas | pp. 5–6 |
| Separação de responsabilidades | cada camada com um papel e regras de comunicação conhecidas | p. 6 |
| Alta coesão, baixo acoplamento | componente com responsabilidade única; mudança num não afeta outros | p. 6 |
| Independência de frameworks | framework só na camada externa, nem na regra de negócio nem na adaptação | pp. 6–7 |
| Dependência em abstração, não em implementação | caso de uso → gateway por interface; gateway → serviço externo por interface | p. 7 |
| Baixo acoplamento a agentes externos | APIs, pagamento, e-mail como detalhes isolados | p. 7 |
| Independência de banco de dados | banco é detalhe, acessado por interface | p. 8 |
| Testabilidade | cada parte testável isolada, com mocks e stubs | p. 8 |
| Manutenibilidade e evolução contínua | modular e preparado para mudar | pp. 8–9 |

A **Aula 01** (p. 7) situa a origem desses princípios nos livros de Martin — SOLID em *Agile
PPP*, código limpo em *Clean Code*, profissionalismo em *The Clean Coder* — e a **Aula 07** (p. 8)
fecha lembrando que separação de responsabilidades e inversão de dependências vêm do SOLID.

## 2. O que os autores dizem

- **Martin, *Agile PPP*** (princípios de design de classes) e ***Clean Architecture*, caps. 7–11**:
  SRP, OCP, LSP, ISP, DIP.
- **Martin, *Clean Architecture*, caps. 12–14**: princípios de **componentes** — coesão (REP, CCP,
  CRP) e acoplamento (ADP, SDP, SAP).
- **Martin, *The Clean Coder***: a qualidade é responsabilidade de quem escreve — incluindo dizer
  "não" a atalhos que a comprometam (a Aula 01, p. 8, destaca esse ponto).

## 3. Como o projeto implementa — princípio → código → verificação

| Princípio | Onde aparece no código | Verificado por |
|---|---|---|
| **SRP** — um motivo para mudar | um caso de uso por intenção; gateway só traduz; presenter só decide o que sai; `SmtpMailSender` só transporta | `ArchitectureTest`: `run` como único método público do caso de uso |
| **OCP** — aberto à extensão, fechado à modificação | feature nova (restaurante) ganha pacotes novos em cada camada; tecnologia nova é um módulo novo ao lado | `InfrastructureModulesTest`: módulos isolados |
| **LSP** — implementações intercambiáveis | qualquer `IUserDataSource` serve ao `UserGateway`: JPA em produção, mock nos testes | testes do adaptador rodam contra mocks das mesmas interfaces |
| **ISP** — interfaces pequenas, do ponto de vista do cliente | `IAccessTokenReader` (só `read`) separada de `ITokenEncoder` (só `encode`); `IMailSender` com um método | — |
| **DIP** — depender de abstração declarada pelo consumidor | portas em `application/gateway`; `I*DataSource` e `adapter/service` no adaptador; `IAccessTokenReader` na web | `ArchitectureTest`: regra de dependência inteira |
| **REP / CCP / CRP** — o que muda junto fica junto | emitir e ler o JWT no mesmo módulo; texto do e-mail com o gateway de e-mail | `InfrastructureModulesTest`: cada biblioteca no seu módulo |
| **ADP** — sem ciclos | o endereço JPA no pacote do agregado de usuário (antes formava ciclo) | `InfrastructureModulesTest`: `nenhum_ciclo_entre_pacotes` no projeto inteiro |
| **SDP / SAP** — depender do estável; o estável é abstrato | `domain` e `application` só têm regras e interfaces e não dependem de nada; a infraestrutura, instável, depende deles | regra de dependência |
| Independência de framework e de banco (Aula 06) | nenhum import de Spring/JPA fora de `infrastructure`; entidades JPA separadas | `ArchitectureTest`: frameworks só em `infrastructure` |
| Testabilidade (Aula 06) | núcleo testado sem Spring nem banco; gate de 100% só dos unitários | JaCoCo `check` + `TestConventionsTest` |
| Nomes que revelam intenção (*Clean Code* cap. 2) | `BearerTokenAuthenticationFilter` (não sabe que é JWT), `restore` × `create`, `SmtpMailSender` | revisão |

## 4. Padrões adotados e por quê

O **princípio orientador** do projeto (registrado no `CLAUDE.md`): as referências são o guia de
toda decisão, e uma regra aceita para um agregado vale para todos. Antes de um atalho — anotação de
framework no núcleo, constante técnica num caso de uso, mapeamento "para economizar" — verifica-se
se ele rompe a integridade entre a arquitetura aplicada e os conceitos; se rompe, não se faz.

## 5. Desvios conscientes

- **Portas técnicas sem gateway** (`IPasswordEncoder`, `ISecureTokenGenerator`, `IUnitOfWork`): o
  princípio de "abstração sobre implementação" da Aula 06 é atendido — o caso de uso depende de
  interface —, mas sem o gateway intermediário, porque não há tradução a fazer. Detalhes em
  [Adaptadores](03-adaptadores.md#5-desvios-conscientes).
- A exceção de dependência entre módulos de infraestrutura: o módulo `token` conhece, da `api`,
  exatamente `IAccessTokenReader` e `AuthenticatedUser` — a porta que implementa e o tipo que ela
  devolve. É a inversão de dependência aplicada dentro da borda, e a regra do build admite essas
  duas classes e nenhuma outra.

## 6. Como o build verifica

Os princípios não ficam só no texto: **44 regras ArchUnit** os fazem quebrar o build quando
violados — 14 no `ArchitectureTest` (regra de dependência e nomenclatura), 23 no
`InfrastructureModulesTest` (módulos, ciclos, bibliotecas, portas técnicas, transporte sem domínio,
organização MVC da API) e 7 no
`TestConventionsTest` (convenções da suíte). Cada regra foi conferida ao contrário, com uma violação
proposital, na etapa em que foi criada.
