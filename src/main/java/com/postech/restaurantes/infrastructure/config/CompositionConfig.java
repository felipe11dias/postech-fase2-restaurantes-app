package com.postech.restaurantes.infrastructure.config;

import com.postech.restaurantes.adapter.controller.AuthController;
import com.postech.restaurantes.adapter.controller.UserController;
import com.postech.restaurantes.adapter.datasource.IPasswordResetTokenDataSource;
import com.postech.restaurantes.adapter.datasource.IRoleDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.application.gateway.IMailGateway;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.ISecureTokenGenerator;
import com.postech.restaurantes.application.gateway.ITokenIssuer;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.infrastructure.mail.MailProperties;
import com.postech.restaurantes.infrastructure.security.JwtProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Raiz de composição: o único ponto onde as peças do núcleo são amarradas às implementações
 * concretas. Os controllers de adaptação são objetos comuns, criados por suas fábricas
 * estáticas e recebendo tudo por interface — não são componentes do Spring, e nem sabem que
 * ele existe. É aqui que a inversão de dependência deixa de ser desenho e vira montagem.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, MailProperties.class})
public class CompositionConfig {

    /** Relógio injetável: nenhum componente chama {@code now()} por conta própria. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public UserController userController(IUserDataSource userDataSource, IRoleDataSource roleDataSource,
                                         IPasswordEncoder passwordEncoder, IUnitOfWork unitOfWork) {
        return UserController.create(userDataSource, roleDataSource, passwordEncoder, unitOfWork);
    }

    @Bean
    public AuthController authController(IUserDataSource userDataSource,
                                         IPasswordResetTokenDataSource tokenDataSource,
                                         IPasswordEncoder passwordEncoder, ITokenIssuer tokenIssuer,
                                         ISecureTokenGenerator tokenGenerator, IMailGateway mailGateway,
                                         MailProperties mailProperties, Clock clock, IUnitOfWork unitOfWork) {
        return AuthController.create(userDataSource, tokenDataSource, passwordEncoder, tokenIssuer, tokenGenerator,
                mailGateway, mailProperties.resetTokenValidity(), clock, unitOfWork);
    }
}
