# Arquitetura Lógica - Tech Challenge Fase 2

Diagrama lógico do sistema no estado da Etapa 29: as quatro camadas da Clean Architecture, com a
dependência sempre apontando para dentro. As setas sólidas são dependências de código; as tracejadas,
implementação de interface (a porta é declarada por quem a consome). O detalhe de cada camada está em
[`docs/arquitetura/`](docs/arquitetura/README.md).

```mermaid
graph TD
    subgraph Camada_Frameworks_Drivers [Frameworks & Drivers - infrastructure]
        API[api/rest/spring<br/>RestControllers, Request/Response, HATEOAS, Swagger, Security/JWT]
        JPA[persistence/jpa<br/>JpaEntities, Spring Data, DataSourceJpa, UnitOfWork]
        MAIN[main<br/>CompositionConfig]
        OUT[token/jwt · crypto · mail/smtp]
        DB[(PostgreSQL<br/>Flyway V1-V17)]
    end

    subgraph Camada_Interface_Adapters [Interface Adapters - adapter]
        CTRL[controller<br/>User, Auth, Restaurant, Product, Cuisine, Location]
        GW[gateway<br/>tradução entidade ↔ Data]
        DS[datasource<br/>I*DataSource]
        SVC[service<br/>IMailSender, ITokenEncoder]
        PRES[presenter<br/>*View]
    end

    subgraph Camada_UseCases [Use Cases - application]
        UC1[Usuários, perfis e autenticação]
        UC2[Restaurantes]
        UC3[Cardápio - produtos]
        UC4[Tipos de cozinha]
        UC5[Catálogo geográfico]
        PORT[gateway<br/>I*Gateway - portas]
    end

    subgraph Camada_Entities [Entities - domain]
        E1((User + perfis<br/>Owner, Client, Courier, Admin))
        E2((Restaurant + OfficeHour<br/>Address))
        E3((Product + opções))
        E4((Cuisine))
        E5((City / State))
    end

    API --> CTRL
    MAIN --> CTRL
    CTRL --> UC1 & UC2 & UC3 & UC4 & UC5
    CTRL --> GW
    CTRL --> PRES
    UC1 --> E1
    UC2 --> E2
    UC3 --> E3
    UC4 --> E4
    UC5 --> E5
    UC1 & UC2 & UC3 & UC4 & UC5 --> PORT
    GW -.implementa.-> PORT
    GW --> DS
    GW --> SVC
    JPA -.implementa.-> DS
    OUT -.implementa.-> SVC
    JPA --> DB
```

O papel do usuário é **derivado dos perfis** (não há mais `Role` nem catálogo de papéis desde a Etapa 21),
e o cardápio é o catálogo de **produtos** de cada restaurante (Etapa 27). Spring Data e JPA ficam em
`infrastructure`; o adaptador só conhece as interfaces `I*DataSource`.

## Visualização Estática (PNG)

A imagem abaixo é a primeira versão do diagrama (setembro de 2026, Etapa 16) e foi mantida como registro
histórico; o diagrama acima é o vigente.

![Diagrama de Arquitetura Lógica](docs/arquitetura-logica.png)
