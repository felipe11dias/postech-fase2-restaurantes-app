package com.postech.restaurantes.infrastructure.config;

import com.postech.restaurantes.infrastructure.web.doc.ApiDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentação OpenAPI e o esquema de segurança Bearer JWT. Puramente declarativa:
 * o que a documentação diz de cada endpoint está nos próprios controllers, e a regra "todo erro
 * é ProblemDetail" está no {@code ProblemDetailOpenApiCustomizer}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Gestão de Restaurantes",
                version = "1.0.0",
                description = """
                        Tech Challenge Fase 2 — Pós-Tech. Cadastro de usuários, autenticação JWT e \
                        recuperação de senha, construídos em Clean Architecture.

                        **Como autenticar**

                        1. Chame `POST /api/v1/auth/login` com um usuário de demonstração — por \
                        exemplo `admin.demo` / `admin12345` (ou `dono.restaurante` / `dono12345`, \
                        `cliente.demo` / `cliente12345`).
                        2. Copie o valor de `token` da resposta.
                        3. Clique em **Authorize** e cole o token, **sem** o prefixo `Bearer`.

                        **Erros** saem sempre em ProblemDetail (RFC 9457). O campo `type` identifica a \
                        categoria (`urn:restaurantes:problema:…`) e é nele que o cliente deve se apoiar; \
                        `detail` traz a explicação para o usuário.
                        """),
        tags = {
                @Tag(name = "Autenticação", description = "Login, esqueci minha senha e redefinição"),
                @Tag(name = "Usuários", description = "Autocadastro e manutenção do próprio cadastro; "
                        + "listagem e acesso a cadastros alheios exigem ROLE_ADMIN")
        })
@SecurityScheme(
        name = ApiDocumentation.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token obtido em POST /api/v1/auth/login. Informe só o token; o prefixo é incluído.")
public class OpenApiConfig {
}
