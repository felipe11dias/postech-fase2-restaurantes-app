package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import com.postech.restaurantes.adapter.controller.AuthController;
import com.postech.restaurantes.infrastructure.api.rest.spring.config.ForgotPasswordConfig;
import com.postech.restaurantes.infrastructure.api.rest.spring.doc.ErrorResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ForgotPasswordRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.LoginRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ResetPasswordRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.AuthResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.exception.ProblemType;
import com.postech.restaurantes.infrastructure.api.rest.spring.route.ApiRoutes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Porta HTTP de autenticação e recuperação de senha. Os três endpoints são públicos — exigir
 * autenticação para fazer login seria circular, e para recuperar a senha, impossível. Por isso
 * nenhum deles declara {@code @SecurityRequirement} na documentação.
 */
@RestController
@RequestMapping(ApiRoutes.AUTH)
@Tag(name = "Autenticação")
public class AuthRestController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthRestController.class);

    private final AuthController controller;
    private final Executor forgotPasswordExecutor;

    public AuthRestController(AuthController controller,
                              @Qualifier(ForgotPasswordConfig.EXECUTOR) Executor forgotPasswordExecutor) {
        this.controller = controller;
        this.forgotPasswordExecutor = forgotPasswordExecutor;
    }

    @PostMapping("/login")
    @Operation(summary = "Login",
            description = "Troca login e senha por um token de acesso JWT. Use o token no botão Authorize. "
                    + "Login inexistente e senha errada recebem a mesma resposta.")
    @ApiResponse(responseCode = "200", description = "Autenticado; o token expira em expiresAt")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Login ou senha ausentes")
    @ErrorResponse(type = ProblemType.AUTHENTICATION_FAILED, description = "Login ou senha incorretos")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthResponse.from(controller.login(request.toDTO()));
    }

    /**
     * Responde {@code 202 Accepted} — e o mesmo {@code 202} — exista ou não o e-mail. Um
     * {@code 404} para endereço desconhecido transformaria este endpoint em um verificador de
     * quem tem conta no sistema.
     *
     * <p>A resposta também precisa levar o <em>mesmo tempo</em>: só o e-mail cadastrado grava um
     * token e fala com o servidor de e-mail, e a diferença de latência revelaria quem tem conta tão
     * bem quanto um 404. Por isso a requisição só valida a sintaxe e entrega o pedido a uma fila; o
     * processamento acontece fora dela. É o que o 202 significa: aceito para processamento.
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Esqueci minha senha",
            description = "Envia por e-mail um token de redefinição de uso único. A resposta é a mesma "
                    + "exista ou não o e-mail — o endpoint não revela quem tem conta.")
    @ApiResponse(responseCode = "202", description = "Pedido aceito")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "E-mail ausente ou malformado")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.email();
        try {
            forgotPasswordExecutor.execute(() -> process(email));
        } catch (RejectedExecutionException e) {
            LOG.warn("Pedido de redefinição de senha descartado: fila cheia");
        }
    }

    /** Fora da requisição não há quem receba a exceção: ela vai para o log, sem o e-mail. */
    private void process(String email) {
        try {
            controller.forgotPassword(email);
        } catch (RuntimeException e) {
            LOG.error("Falha ao processar pedido de redefinição de senha", e);
        }
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Redefine a senha", description = "Usa o token recebido por e-mail. O token vale uma vez só.")
    @ApiResponse(responseCode = "204", description = "Senha redefinida")
    @ErrorResponse(type = ProblemType.INVALID_TOKEN, description = "Token inválido, expirado ou já usado")
    @ErrorResponse(type = ProblemType.INVALID_PASSWORD, description = "Confirmação divergente da senha nova")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Campo ausente ou senha nova fora das regras de tamanho")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        controller.resetPassword(request.toDTO());
    }
}
