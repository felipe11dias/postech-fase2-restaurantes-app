# Relatório Acadêmico de Implementação — Módulo de Gestão de Restaurantes

**Autor do Módulo:** Aluno / Integrante da Equipe (Francisco)  
**Curso:** Pós-Tech em Arquitetura e Desenvolvimento Java (Tech Challenge — Fase 2)  
**Módulo Desenvolvido:** Agregado de Restaurante (`restaurants`) — Imagem 2 do Modelo ER  
**Data:** Setembro/2026  

---

## 1. Introdução e Contextualização do Projeto

No âmbito do Tech Challenge (Fase 2) da Pós-Tech em Arquitetura e Desenvolvimento Java, a equipe foi incumbida de construir a base do sistema de gestão compartilhada de restaurantes aplicando os princípios de **Clean Architecture** (Robert C. Martin), padrões **SOLID**, governança de banco de dados relacional e cobertura rigorosa de testes automatizados.

Dentro da divisão de trabalho da equipe, coube a este autor a responsabilidade pelo design, modelagem, implementação e testes do **Módulo de Gestão de Restaurantes** (representado pela tabela e entidade `restaurants`, conforme especificado na Imagem 2 do modelo ER).

Este documento detalha o que foi desenvolvido, como o módulo foi estruturado dentro da arquitetura em camadas concêntricas e as justificativas técnicas e acadêmicas para as decisões de projeto tomadas.

---

## 2. O que o Módulo Contém (Escopo Técnico Entregue)

O módulo desenvolvido compreende a implementação vertical completa do agregado de restaurante (*screaming architecture*), englobando as 4 camadas concêntricas da Clean Architecture:

1. **Camada de Domínio (`domain/entity/restaurant`)**:
   - Entidade Raiz `Restaurant`: encapsulamento total do estado e aplicação rigorosa das invariantes de negócio (nome não-vazio, IDs de dono e endereço válidos, horários de funcionamento coerentes).
   - Métodos estáticos de fábrica `create` (para novas instâncias) e `restore` (para reconstituição a partir da persistência).

2. **Camada de Casos de Uso (`application/usecase/restaurant`)**:
   - `CreateRestaurantUseCase`: criação de restaurante verificando se o usuário possui perfil autorizado (`ROLE_OWNER` ou `ROLE_ADMIN`) e se o endereço pertence ao usuário.
   - `FindRestaurantByIdUseCase`: busca individual por identificador único (UUID).
   - `SearchRestaurantsUseCase`: listagem paginada com suporte a filtros e ordenação sanitizada.
   - `UpdateRestaurantUseCase`: atualização de dados cadastrais com revalidação de regras de negócio.
   - `DeleteRestaurantUseCase`: remoção segura do registro de restaurante.
   - Interface de porta `IRestaurantGateway` e DTOs imutáveis (`CreateRestaurantDTO`, `UpdateRestaurantDTO`).

3. **Camada de Adaptadores de Interface (`adapter`)**:
   - `RestaurantController`: orquestração entre requisições externas, casos de uso, transacionalidade (`IUnitOfWork`) e a camada de apresentação.
   - `RestaurantGateway`: tradutor isolado entre o núcleo de domínio (`Restaurant`) e o modelo de dados da infraestrutura (`RestaurantData`).
   - `RestaurantPresenter` e `RestaurantView`: formatação da visão exposta para o canal HTTP.
   - Interface de origem de dados `IRestaurantDataSource`.

4. **Camada de Infraestrutura e Drivers (`infrastructure`)**:
   - **Banco de Dados & Migração**: Script Flyway `V3__create_restaurant_schema.sql` definindo a tabela `restaurants`, chaves estrangeiras (`user_id`, `address_id`) e índices de busca.
   - **Persistência JPA**: Entidade de mapeamento `RestaurantJpaEntity` (herdando auditabilidade automática com `@CreatedDate` e `@LastModifiedDate`), repositório `SpringDataRestaurantRepository` e adaptador `RestaurantDataSourceJpa`.
   - **API REST & Spring MVC**: `RestaurantRestController` expondo os endpoints REST (`POST`, `GET`, `PUT`, `DELETE` em `/api/v1/restaurants`), DTOs sintáticos com Bean Validation (`CreateRestaurantRequest`, `UpdateRestaurantRequest`), respostas HATEOAS (`RestaurantModelAssembler`, `RestaurantResponse`) e integração com OpenAPI/Swagger.
   - **Segurança & Injeção**: Configurações em `SecurityConfig` (liberação de consulta e restrição de escrita para `ROLE_OWNER` e `ROLE_ADMIN`) e registro de beans em `CompositionConfig`.

5. **Testes Automatizados & Qualidade de Código**:
   - Testes unitários para 100% das classes e ramificações condicionais criadas.
   - Teste de integração de ponta a ponta (`RestaurantLifecycleIT`) utilizando banco PostgreSQL real via **Testcontainers**.
   - Validação da arquitetura sem violações de dependência através de regras **ArchUnit**.

---

## 3. Justificativas Acadêmicas e Decisões de Projeto (*Por que fiz assim?*)

### 3.1. Respeito à Regra de Dependência (Clean Architecture - Robert C. Martin)
A principal motivação para separar a entidade de domínio `Restaurant` da entidade de persistência `RestaurantJpaEntity` foi evitar o acoplamento do modelo de negócio com frameworks de ORM (Hibernate/JPA). 
- O domínio não possui anotações de framework (`@Entity`, `@Table`, `@Column`).
- Alterações em bibliotecas externas ou no banco de dados não afetam as regras de negócio de restaurantes.

### 3.2. Princípio da Responsabilidade Única (SRP - SOLID)
Cada caso de uso (`CreateRestaurantUseCase`, `FindRestaurantByIdUseCase`, etc.) foi implementado em uma classe dedicada que expõe o método `run` como único ponto de entrada público. Isso atende à máxima de Cockburn e Martin de que uma classe de aplicação representa uma única intenção do ator do sistema.

### 3.3. Inversão de Dependência e Portas/Adaptadores (DIP - Alistair Cockburn)
A camada de aplicação declara a interface `IRestaurantGateway` (o que ela necessita para funcionar), e o adaptador `RestaurantGateway` implementa essa interface traduzindo os objetos de domínio em `RestaurantData`. A infraestrutura `RestaurantDataSourceJpa` apenas cumpre o contrato de acesso ao banco relacional.

### 3.4. Integridade de Dados no Banco Relacional (Normas de Date e Machado)
Na modelagem relacional (`V3__create_restaurant_schema.sql`), foram aplicadas as regras formais de normalização (1FN a BCNF):
- O relacionamento entre `restaurants` e `users` impede a existência de restaurantes sem dono responsável.
- O vínculo com `addresses` reaproveita a estrutura de localização existente no sistema.
- Restrições de integridade referencial (`ON DELETE RESTRICT`) garantem que usuários ou endereços vinculados a um restaurante ativo não sejam acidentalmente removidos.

### 3.5. Garantia de Qualidade por Cobertura Total de Testes
Para assegurar a robustez da solução, o módulo foi submetido ao gate de qualidade do JaCoCo (exigindo 100% de cobertura de linhas e ramos nos testes unitários) e validado com PostgreSQL real através do Testcontainers, provando a viabilidade de execução do código em ambiente real de produção conteinerizado.

---

## 4. Conclusão

A implementação do **Módulo de Gestão de Restaurantes** cumpre integralmente os requisitos funcionais solicitados para a entidade `restaurants`, mantendo total conformidade com a arquitetura padrão adotada pela equipe no repositório. O código entregue é modular, testado, extensível e pronto para ser integrado ao repositório principal via Pull Request.
