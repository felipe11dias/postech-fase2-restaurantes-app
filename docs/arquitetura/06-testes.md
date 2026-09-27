# 6. Testes — como cada camada é provada

[← Princípios](05-principios.md) · [Índice](README.md)

## 1. O que a aula ensina

A **Aula 04** aplica o Clean Code aos testes (pp. 5–11):

- **nomes descritivos** que dizem o que se testa e em que condição — a aula dá o exemplo de um
  nome no formato "deve cadastrar corretamente…", em vez de "testCadastro" (pp. 5, 8);
- **funções curtas com uma responsabilidade**, e um teste focado num comportamento, sem lógica
  complexa nem várias verificações sem relação (pp. 5–8);
- **DRY** e **exceções** em vez de códigos de retorno (p. 7);
- testes **isolados e independentes**, sem estado compartilhado, com dependências externas
  simuladas por *mocks* (pp. 8–9);
- testes **rápidos**, para serem executados sempre, e **refatorados** junto com o código (p. 9);
- a estrutura **Arrange, Act, Assert** (pp. 9–11).

A **Aula 03** mostra os testes das duas camadas internas — entidade testada sem mocks (pp. 9–11),
caso de uso com o gateway mockado (pp. 13–14) — e a **Aula 06** (p. 8) põe a testabilidade entre os
objetivos da arquitetura.

## 2. O que os autores dizem

- **Martin, *Clean Code*, cap. 9 (*Unit Tests*)**: testes limpos são tão importantes quanto o código
  de produção; regras **F.I.R.S.T.** — *Fast, Independent, Repeatable, Self-validating, Timely*; um
  conceito por teste.
- **Martin, *The Clean Coder*, cap. 8 (*Testing Strategies*)**: a pirâmide de automação — muitos
  testes unitários na base, testes de integração de componentes acima, poucos de ponta a ponta no
  topo.
- **Martin, *Clean Architecture*, cap. 28 (*The Test Boundary*)**: testes são o círculo mais
  externo da arquitetura e devem depender do sistema só por interfaces estáveis.

## 3. Como o projeto implementa

| Nível | Onde | Como | Quantidade |
|---|---|---|---|
| Entidades | `src/test/.../domain` | JUnit sem mocks, um teste para o valor aceito e outro para o recusado | parte dos unitários |
| Casos de uso | `src/test/.../application` | Mockito nas portas `I*Gateway`, `Clock.fixed` para o tempo | parte dos unitários |
| Adaptadores | `src/test/.../adapter` | mocks de `I*DataSource` e de `adapter/service`; controllers com casos de uso, gateways e presenters **reais** | parte dos unitários |
| Infraestrutura com lógica | `src/test/.../infrastructure` | classes instanciadas direto, com `JpaRepository`, `SecurityContext`, `MailSender` mockados | parte dos unitários |
| Arquitetura e convenções | `ArchitectureTest`, `InfrastructureModulesTest`, `TestConventionsTest` | ArchUnit | 37 regras |
| Integração | `*IT` | `@SpringBootTest`, PostgreSQL real (Testcontainers), Flyway, JWT ativo, HTTP de verdade; só o SMTP é substituído | 91 |
| Aceitação manual/automatizada | `postman/` | coleção com um request por caso de cada endpoint, rodada com Newman | 52 requests |

**Os dois compromissos**, ambos verificados no build:

1. **100% de cobertura de linhas e ramos pelos testes unitários.** O JaCoCo tem agentes separados
   para unitários (`jacoco.exec`, que alimenta o `check`) e integração (`jacoco-it.exec`,
   informativo): uma linha coberta só por teste de integração não conta.
2. **Integração que prova os componentes reais juntos**, sem substituir nenhum bean da aplicação.

**Convenções** (Aula 04 e *Clean Code* cap. 9), verificadas pelo
[`TestConventionsTest`](../../src/test/java/com/postech/restaurantes/TestConventionsTest.java):
`@DisplayName` em todo teste; nome `deve<Comportamento>[Quando<Condição>]` ou `naoDeve…`; teste
unitário sem Spring, Testcontainers nem JDBC; teste de integração estende uma das bases; `@MockitoBean`
só no `JavaMailSender`; nenhum `@MockitoSpyBean`; nenhum `@Testcontainers`.

**Independência** (F.I.R.S.T., Aula 04 p. 8): o banco dos testes de integração é compartilhado e não
é limpo; cada teste cria os próprios dados com uma marca única e consulta só por ela. A suíte foi
executada em **ordem aleatória** de classes e métodos (duas sementes) para provar isso.

Exemplo — o mesmo formato AAA da Aula 04:

```java
@Test
@DisplayName("Auditoria grava o autor de cada gravação: system no autocadastro, depois quem alterou")
void deveGravarOAutorDeCadaAlteracao() {
    Usuario eu = cadastrarEAutenticar();                            // arrange
    atualizar(eu, "Alterado Pelo Proprio", eu.token());             // act
    assertEquals(List.of("system", eu.login()), autoria(eu.id()));  // assert
    ...
}
```

## 4. Padrões adotados e por quê

| Padrão | Onde | Problema que resolve |
|---|---|---|
| Pirâmide (Clean Coder cap. 8) | 458 unitários × 91 integração × 52 requests | feedback rápido na base; confiança de ponta a ponta no topo |
| Mock da porta, não da implementação | casos de uso e adaptadores | o teste depende só de interfaces estáveis (CA cap. 28) |
| Controle em teste negativo | `JwtAuthenticationIT`: o mesmo formato de token com a chave certa responde 200 | a recusa é provada pelo motivo certo, não por um token malformado pelo teste |
| Conferência ao contrário | cada regra ArchUnit com uma violação proposital | uma regra que nunca falha pode não estar verificando nada |
| Container único e compartilhado | `SharedPostgres` | contexto Spring em cache sempre aponta para um banco vivo |

## 5. Desvios conscientes

| Na aula | No projeto | Por quê |
|---|---|---|
| "Evitar várias verificações num teste" (Aula 04, p. 8) | `UserLifecycleIT` percorre o cadastro inteiro num teste | é o cenário principal de sucesso encadeado (Cockburn): o objetivo é provar que o estado deixado por um passo serve ao seguinte — os passos isolados já têm testes próprios |
| Nomes de teste no exemplo da Aula 03 (`testEstudanteOk`) | `deve…` / `naoDeve…` | segue a recomendação da Aula 04, que é posterior e explícita |
| Testes isolados "sem compartilhar estado" | banco compartilhado entre classes de integração | isolar por transação não alcança requisições HTTP (outra thread, outra transação); marcas únicas + ordem aleatória garantem a independência |

## 6. Como o build verifica

`mvn verify` falha se: algum teste falhar; a cobertura unitária de linhas ou ramos ficar abaixo de
100%; qualquer uma das 37 regras ArchUnit for violada. `mvn test` roda só os unitários e as regras,
em segundos, sem Docker.
