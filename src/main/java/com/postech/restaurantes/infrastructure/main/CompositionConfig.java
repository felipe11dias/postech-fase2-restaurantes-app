package com.postech.restaurantes.infrastructure.main;

import com.postech.restaurantes.adapter.controller.AuthController;
import com.postech.restaurantes.adapter.controller.RestaurantController;
import com.postech.restaurantes.adapter.controller.UserController;
import com.postech.restaurantes.adapter.datasource.IPasswordResetTokenDataSource;
import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.service.IMailSender;
import com.postech.restaurantes.adapter.service.ITokenEncoder;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.ISecureTokenGenerator;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuthenticatedAuditorAware;
import com.postech.restaurantes.infrastructure.api.rest.spring.security.AuthenticatedActor;
import com.postech.restaurantes.infrastructure.api.rest.spring.security.ICurrentRolesReader;
import com.postech.restaurantes.infrastructure.api.rest.spring.security.IRestaurantOwnerReader;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

/**
 * Raiz de composição — o componente Main (Martin, Clean Architecture cap. 26): o único ponto que
 * conhece todos os módulos. Os controllers de adaptação são objetos comuns, criados por suas
 * fábricas estáticas e recebendo tudo por interface — não são componentes do Spring, e nem sabem
 * que ele existe. É aqui que a inversão de dependência deixa de ser desenho e vira montagem.
 *
 * <p>Também é aqui que um módulo recebe o que outro sabe, sem que um importe o outro: o autor da
 * auditoria (persistência) vem do usuário autenticado (segurança HTTP). Cada módulo habilita a
 * própria configuração; esta classe não conhece propriedades de tecnologia nenhuma.
 */
@Configuration
@EnableConfigurationProperties(PasswordResetProperties.class)
public class CompositionConfig {

    /** Relógio injetável: nenhum componente chama {@code now()} por conta própria. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Nome referenciado pelo {@code @EnableJpaAuditing} do módulo JPA. */
    @Bean("authenticatedAuditorAware")
    public AuditorAware<String> authenticatedAuditorAware(AuthenticatedActor actor) {
        return new AuthenticatedAuditorAware(actor::currentLogin);
    }

    @Bean
    public UserController userController(IUserDataSource userDataSource,
                                         IRestaurantDataSource restaurantDataSource,
                                         IPasswordEncoder passwordEncoder, IUnitOfWork unitOfWork, Clock clock) {
        return UserController.create(userDataSource, restaurantDataSource, passwordEncoder, unitOfWork, clock);
    }

    @Bean
    public AuthController authController(IUserDataSource userDataSource,
                                         IPasswordResetTokenDataSource tokenDataSource,
                                         IPasswordEncoder passwordEncoder, ITokenEncoder tokenEncoder,
                                         ISecureTokenGenerator tokenGenerator, IMailSender mailSender,
                                         PasswordResetProperties passwordReset, Clock clock, IUnitOfWork unitOfWork) {
        return AuthController.create(userDataSource, tokenDataSource, passwordEncoder, tokenEncoder, tokenGenerator,
                mailSender, passwordReset.tokenValidity(), clock, unitOfWork);
    }

    /**
     * Os papéis de cada requisição saem do cadastro, pelo caso de uso de autenticação: a cadeia HTTP declara a
     * porta ({@link ICurrentRolesReader}) e não conhece o núcleo; a composição liga as pontas.
     */
    @Bean
    public ICurrentRolesReader currentRolesReader(AuthController authController) {
        return authController::currentRoles;
    }

    @Bean
    public RestaurantController restaurantController(IRestaurantDataSource restaurantDataSource,
                                                     IUserDataSource userDataSource, IUnitOfWork unitOfWork) {
        return RestaurantController.create(restaurantDataSource, userDataSource, unitOfWork);
    }

    /** A regra de posse do restaurante pergunta o dono pelo núcleo; a API declara a porta, e a composição liga. */
    @Bean
    public IRestaurantOwnerReader restaurantOwnerReader(RestaurantController restaurantController) {
        return restaurantController::ownerOf;
    }
}
