# Arquitetura do projeto — documentação por camada

Esta pasta explica **cada parte da Clean Architecture** como ela foi aplicada neste projeto,
correlacionando três fontes:

1. **As aulas da Fase 2** (Pós-Tech FIAP — Arquitetura e Desenvolvimento Java, Aulas 01 a 07), que
   ensinam a arquitetura com um roteiro concreto de implementação em Java.
2. **Os autores de referência** do relatório técnico — Robert C. Martin (*Clean Architecture*,
   *Clean Code*, *Agile Principles, Patterns, and Practices*, *The Clean Coder*), Alistair Cockburn
   (*Writing Effective Use Cases*), Eric Freeman e Elisabeth Robson (*Head First Design Patterns*),
   C. J. Date e Felipe Machado (bancos de dados).
3. **O código** — pacotes e classes reais, com links.

As aulas são material do curso: aqui elas são **resumidas com palavras próprias** e citadas por
aula e página, nunca transcritas.

## Como ler

Comece pela visão geral e siga de dentro para fora, na ordem da regra de dependência:

| # | Documento | Parte da arquitetura | Aulas | Pacotes |
|---|---|---|---|---|
| 0 | [Visão geral](00-visao-geral.md) | as quatro camadas e o fluxo de uma requisição | 01, 02, 07 | todos |
| 1 | [Entidades](01-entidades.md) | *Enterprise Business Rules* | 02, 03 | `domain` |
| 2 | [Casos de uso](02-casos-de-uso.md) | *Application Business Rules* | 02, 03 | `application` |
| 3 | [Adaptadores de interface](03-adaptadores.md) | controllers, gateways, presenters | 02, 05, 07 | `adapter` |
| 4 | [Frameworks & Drivers](04-frameworks-drivers.md) | os detalhes: web, banco, token, e-mail | 02, 06 | `infrastructure` |
| 5 | [Princípios](05-principios.md) | o que sustenta tudo: SOLID, componentes, dependência | 06 | — |
| 6 | [Testes](06-testes.md) | como cada camada é provada | 04 | `src/test` |

## Estrutura de cada documento

Todos seguem as mesmas seis seções, para que a comparação entre camadas seja direta:

1. **O que a aula ensina** — o resumo do conteúdo, com aula e página.
2. **O que os autores dizem** — livro e capítulo.
3. **Como o projeto implementa** — onde está no código.
4. **Padrões adotados e por quê** — o nome do padrão, onde aparece e o problema que resolve.
5. **Desvios conscientes** — onde o projeto difere do exemplo da aula, e a razão.
6. **Como o build verifica** — as regras (ArchUnit) e os testes que impedem a camada de regredir.

## Relação com o resto do repositório

- O **relatório técnico** (`relatorios/relatorio-tech-challenge-fase02-v1.0.md`) conta a história
  do projeto etapa por etapa — o que foi decidido, quando e por quê. Esta pasta descreve o
  **estado atual**, organizado por camada.
- Cada `package-info.java` das camadas e sub-pacotes principais traz um resumo do papel do pacote
  e aponta para o documento correspondente.
- Quando uma decisão ou um padrão mudar, o documento da camada muda no mesmo commit (regra do
  `CLAUDE.md`).
